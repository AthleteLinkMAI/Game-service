package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import team.anonyms.athletelink.gameservice.repositories.GameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerGameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.util.HashSet;
import java.util.List;
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

    public GameResultService(
        GameResultRepository gameResultRepository,
        PlayerGameResultRepository playerGameResultRepository,
        PlayerProfileService playerProfileService,
        SportRepository sportRepository
    ) {
        this.gameResultRepository = gameResultRepository;
        this.playerGameResultRepository = playerGameResultRepository;
        this.playerProfileService = playerProfileService;
        this.sportRepository = sportRepository;
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
     * @param organizerId ID of the caller from the X-User-Id header; it is kept
     *                    for the authorization boundary and is not persisted
     * @return The ID of the created game result
     * @throws IllegalArgumentException if a result for this activity request already exists
     */
    @Transactional
    public UUID submitGameResult(SubmitGameResultDto dto, UUID organizerId) {

        Set<UUID> playerIds = new HashSet<>();
        for (SubmitGameResultDto.PlayerResultEntry player : dto.players()) {
            if (!playerIds.add(player.userId())) {
                throw new IllegalArgumentException("A player cannot appear twice in one game result");
            }
        }

        gameResultRepository.findByActivityRequestId(dto.activityRequestId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Game result for this activity request has already been submitted");
                });

        Sport sport = sportRepository.findById(dto.sportId())
                .orElseThrow(() -> new IllegalArgumentException("Sport not found: " + dto.sportId()));

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
     * Gets the details of a specific game result, including all individual player results.
     *
     * @param gameResultId ID of the game result
     * @throws IllegalArgumentException if the game result is not found.
     */
    public GameResultDetailsDto getGameResultDetails(UUID gameResultId) {
        GameResult gameResult = gameResultRepository.findById(gameResultId)
                .orElseThrow(() -> new IllegalArgumentException("Game result not found"));

        List<PlayerGameResult> playerResults = playerGameResultRepository
                .findAllByGameResultId(gameResultId);

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
     * Gets the match history for a specific player, sorted by most recent first.
     *
     * @param playerProfileId ID of the player profile
     * @return A list of game history entries
     */
    public List<PlayerGameHistoryDto> getPlayerHistory(UUID playerProfileId) {
        return playerGameResultRepository
                .findAllByPlayerProfileIdOrderByGameResult_CreatedAtDesc(playerProfileId)
                .stream()
                .map(pgr -> new PlayerGameHistoryDto(
                        pgr.getGameResult().getId(),
                        pgr.getGameResult().getSport().getId(),
                        pgr.getOutcome(),
                        pgr.getRatingChange(),
                        pgr.getGameResult().getCreatedAt()
                ))
                .toList();
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
