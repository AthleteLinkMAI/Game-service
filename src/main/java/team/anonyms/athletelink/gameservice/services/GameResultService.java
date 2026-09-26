package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.GameResultDetailsDto;
import team.anonyms.athletelink.gameservice.dto.PlayerGameHistoryDto;
import team.anonyms.athletelink.gameservice.dto.SubmitGameResultDto;
import team.anonyms.athletelink.gameservice.entities.GameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerGameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;
import team.anonyms.athletelink.gameservice.exceptions.ConflictException;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.MatchHistoryCursor;
import team.anonyms.athletelink.gameservice.repositories.GameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerGameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerHistoryRowProjection;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Service class for managing game results, including submitting match results,
 * computing per-sport rating changes and retrieving game history.
 */
@Service
public class GameResultService {

    private final GameResultRepository gameResultRepository;
    private final PlayerGameResultRepository playerGameResultRepository;
    private final PlayerProfileService playerProfileService;
    private final SportRepository sportRepository;
    private final CursorService cursorService;

    private static final String HISTORY_CURSOR_TYPE = "match-history";
    private static final int MAX_PAGE_SIZE = 100;

    public GameResultService(
        GameResultRepository gameResultRepository,
        PlayerGameResultRepository playerGameResultRepository,
        PlayerProfileService playerProfileService,
        SportRepository sportRepository,
        CursorService cursorService
    ) {
        this.gameResultRepository = gameResultRepository;
        this.playerGameResultRepository = playerGameResultRepository;
        this.playerProfileService = playerProfileService;
        this.sportRepository = sportRepository;
        this.cursorService = cursorService;
    }

    /**
     * Submits a game result for a completed match.
     *
     * Business rules:
     * - A game result for the same activity request cannot be submitted twice.
     * - At least one player result must be provided.
     * - Player profiles and per-sport statistics are auto-created if necessary.
     * - Rating changes are centralized in calculateRatingChange until the final
     *   rating formula is approved.
     * - Wins, losses and draws are updated in the selected sport.
     *
     * @param dto Data for the game result submission
     * @return The ID of the created game result
     * @throws team.anonyms.athletelink.gameservice.exceptions.ConflictException
     *         if a result for this activity request already exists
    */
    @Transactional
    public UUID submitGameResult(SubmitGameResultDto dto) {

        if (dto == null || dto.players() == null || dto.players().isEmpty()) {
            throw new IllegalArgumentException("At least one player result is required");
        }

        Set<UUID> playerIds = new HashSet<>();
        for (SubmitGameResultDto.PlayerResultEntry player : dto.players()) {
            if (!playerIds.add(player.userId())) {
                throw new IllegalArgumentException("A player cannot appear twice in one game result");
            }
        }

        gameResultRepository.findByActivityRequestId(dto.activityRequestId())
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Game result for this activity request has already been submitted");
                });

        Sport sport = sportRepository.findById(dto.sportId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sport not found: " + dto.sportId()));

        if (dto.players().size() < sport.getMinPlayers()
                || dto.players().size() > sport.getMaxPlayers()) {
            throw new IllegalArgumentException(
                    "Player count must be between "
                            + sport.getMinPlayers()
                            + " and "
                            + sport.getMaxPlayers()
            );
        }

        GameResult gameResult = GameResult.builder()
                .activityRequestId(dto.activityRequestId())
                .sport(sport)
                .status(GameResultStatus.CONFIRMED)
                .build();

        gameResultRepository.save(gameResult);

        List<PlayerSportStats> sportStats = dto.players().stream()
                .map(player -> playerProfileService.getOrCreateSportStats(
                        playerProfileService.getOrCreateProfile(player.userId()),
                        sport
                ))
                .toList();

        for (int index = 0; index < dto.players().size(); index++) {
            SubmitGameResultDto.PlayerResultEntry entry = dto.players().get(index);
            PlayerProfile profile = playerProfileService.getOrCreateProfile(entry.userId());
            PlayerSportStats stats = sportStats.get(index);

            double opponentAverageScore = sportStats.stream()
                    .filter(candidate -> candidate != stats)
                    .mapToInt(PlayerSportStats::getScore)
                    .average()
                    .orElse(stats.getScore());
            int ratingChange = calculateRatingChange(
                    stats.getScore(),
                    opponentAverageScore,
                    entry.outcome()
            );

            PlayerGameResult playerGameResult = PlayerGameResult.builder()
                    .gameResult(gameResult)
                    .playerProfile(profile)
                    .outcome(entry.outcome())
                    .ratingChange(ratingChange)
                    .build();

            playerGameResultRepository.save(playerGameResult);

            stats.setScore(Math.max(0, stats.getScore() + ratingChange));

            switch (entry.outcome()) {
                case WIN -> stats.setWins(stats.getWins() + 1);
                case LOSS -> stats.setLosses(stats.getLosses() + 1);
                case DRAW -> stats.setDraws(stats.getDraws() + 1);
            }

            playerProfileService.saveSportStats(stats);
        }

        return gameResult.getId();
    }

    /**
     * Finds the result by the activity request that produced the game.
     */
    @Transactional(readOnly = true)
    public GameResultDetailsDto getGameResultByActivityRequestId(UUID activityRequestId) {
        GameResult gameResult = gameResultRepository.findByActivityRequestId(activityRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Game result not found for activity request: " + activityRequestId));

        return toDetailsDto(gameResult);
    }

    private GameResultDetailsDto toDetailsDto(GameResult gameResult) {
        List<PlayerGameResult> playerResults = playerGameResultRepository
                .findAllByGameResultId(gameResult.getId());

        List<GameResultDetailsDto.PlayerResultDto> playerDtos = playerResults.stream()
                .map(pr -> new GameResultDetailsDto.PlayerResultDto(
                        pr.getPlayerProfile().getUserId(),
                        pr.getOutcome(),
                        pr.getRatingChange()
                ))
                .toList();

        return new GameResultDetailsDto(
                gameResult.getId(),
                gameResult.getActivityRequestId(),
                gameResult.getSport().getId(),
                gameResult.getStatus(),
                gameResult.getCreatedAt(),
                playerDtos
        );
    }

    /**
     * Public read operation for the future /players/{userId}/matches endpoint.
     * The controller passes a user ID; profile-ID resolution stays in services.
     */
    @Transactional(readOnly = true)
    public CursorPageDto<PlayerGameHistoryDto> getPlayerMatches(
            UUID userId,
            Long sportId,
            String cursor,
            int limit
    ) {
        UUID profileId = playerProfileService.getProfileId(userId);
        return getPlayerHistory(profileId, sportId, cursor, limit);
    }

    /**
     * Returns a cursor-paginated history for an existing player profile.
     * The profile ID is used here because it is the foreign key stored in
     * player_game_results. External callers should use getPlayerMatches,
     * which resolves a user ID to the local profile ID.
     *
     * @param playerProfileId player_profiles.id
     * @param sportId optional sport filter
     * @param cursor opaque cursor from the previous response, or {@code null}
     * @param limit requested page size
     */
    @Transactional(readOnly = true)
    public CursorPageDto<PlayerGameHistoryDto> getPlayerHistory(
            UUID playerProfileId,
            Long sportId,
            String cursor,
            int limit
    ) {
        validateLimit(limit);

        if (sportId != null) {
            sportRepository.findById(sportId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sport not found: " + sportId));
        }

        MatchHistoryCursor decodedCursor = decodeHistoryCursor(
                cursor,
                playerProfileId,
                sportId
        );

        List<PlayerHistoryRowProjection> rows = playerGameResultRepository.findHistory(
                playerProfileId,
                sportId,
                decodedCursor == null ? null : decodedCursor.createdAt(),
                decodedCursor == null ? null : decodedCursor.gameResultId(),
                limit + 1
        );

        boolean hasNext = rows.size() > limit;
        List<PlayerHistoryRowProjection> pageRows = hasNext
                ? rows.subList(0, limit)
                : rows;

        List<PlayerGameHistoryDto> items = pageRows.stream()
                .map(this::toHistoryDto)
                .toList();

        String nextCursor = hasNext
                ? encodeNextHistoryCursor(
                        pageRows.get(pageRows.size() - 1),
                        playerProfileId,
                        sportId
                )
                : null;

        return new CursorPageDto<>(items, nextCursor, hasNext);
    }

    private MatchHistoryCursor decodeHistoryCursor(
            String cursor,
            UUID playerProfileId,
            Long sportId
    ) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        MatchHistoryCursor decoded = cursorService.decode(
                cursor,
                MatchHistoryCursor.class
        );
        if (!HISTORY_CURSOR_TYPE.equals(decoded.type())
                || !playerProfileId.equals(decoded.playerProfileId())
                || !Objects.equals(sportId, decoded.sportId())
                || decoded.createdAt() == null
                || decoded.gameResultId() == null) {
            throw new InvalidCursorException("Cursor does not belong to match history");
        }
        return decoded;
    }

    private PlayerGameHistoryDto toHistoryDto(PlayerHistoryRowProjection row) {
        return new PlayerGameHistoryDto(
                row.getGameResultId(),
                row.getSportId(),
                GameOutcome.valueOf(row.getOutcome()),
                row.getRatingChange(),
                row.getCreatedAt()
        );
    }

    private String encodeNextHistoryCursor(
            PlayerHistoryRowProjection lastRow,
            UUID playerProfileId,
            Long sportId
    ) {
        return cursorService.encode(new MatchHistoryCursor(
                HISTORY_CURSOR_TYPE,
                playerProfileId,
                sportId,
                lastRow.getCreatedAt(),
                lastRow.getGameResultId()
        ));
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * Placeholder for the approved per-sport rating formula.
     *
     * @param playerScore Current score of the player in this sport
     * @param opponentAverageScore Average score of the other participants
     * @param outcome The match outcome (WIN/LOSS/DRAW)
     * @return The rating change (can be negative)
     */
    private int calculateRatingChange(int playerScore, double opponentAverageScore, GameOutcome outcome) {
        // TODO: replace with the approved rating formula. DRAW remains a valid
        // outcome and must be handled by the final formula.
        return 0;
    }
}
