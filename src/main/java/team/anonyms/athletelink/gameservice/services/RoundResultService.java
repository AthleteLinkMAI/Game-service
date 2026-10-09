package team.anonyms.athletelink.gameservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.PlayerRoundHistoryDto;
import team.anonyms.athletelink.gameservice.dto.RoundResultDetailsDto;
import team.anonyms.athletelink.gameservice.dto.SubmitRoundResultDto;
import team.anonyms.athletelink.gameservice.dto.kafka.GameReply;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerRoundResult;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.RoundResult;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.RoundHistoryCursor;
import team.anonyms.athletelink.gameservice.repositories.PlayerHistoryRowProjection;
import team.anonyms.athletelink.gameservice.repositories.PlayerRoundResultRepository;
import team.anonyms.athletelink.gameservice.repositories.RoundResultRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoundResultService {
    private static final String HISTORY_CURSOR_TYPE = "round-history";
    private static final int MAX_PAGE_SIZE = 100;

    private final RoundResultRepository roundResultRepository;
    private final PlayerRoundResultRepository playerRoundResultRepository;
    private final PlayerProfileService playerProfileService;
    private final CursorService cursorService;
    private final KafkaProducerService kafkaProducerService;

    /** One Kafka event is one round; all writes commit or roll back together. */
    @Transactional
    public UUID submitRoundResult(SubmitRoundResultDto dto) {
        validateResult(dto);

        var existing = roundResultRepository.findByActivityRequestIdAndRoundNumber(
                dto.activityRequestId(), dto.roundNumber());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        Map<UUID, PlayerProfile> profiles = new HashMap<>();
        dto.players().stream()
                .map(SubmitRoundResultDto.PlayerResultEntry::userId)
                .sorted()
                .forEach(userId -> profiles.put(
                        userId, playerProfileService.requireProfileForUpdate(userId)));

        Map<UUID, PlayerSportStats> statsByUser = new HashMap<>();
        Map<UUID, Integer> scoresBeforeRound = new HashMap<>();
        for (SubmitRoundResultDto.PlayerResultEntry player : dto.players()) {
            PlayerSportStats stats = playerProfileService.getOrCreateSportStats(
                    profiles.get(player.userId()), dto.sportId());
            statsByUser.put(player.userId(), stats);
            scoresBeforeRound.put(player.userId(), stats.getScore());
        }

        RoundResult round = roundResultRepository.save(RoundResult.builder()
                .activityRequestId(dto.activityRequestId())
                .roundNumber(dto.roundNumber())
                .sportId(dto.sportId())
                .status(GameResultStatus.CONFIRMED)
                .build());

        for (SubmitRoundResultDto.PlayerResultEntry player : dto.players()) {
            int oldScore = scoresBeforeRound.get(player.userId());
            double opponentAverage = scoresBeforeRound.entrySet().stream()
                    .filter(candidate -> !candidate.getKey().equals(player.userId())) // TODO: fix to the average of opponent team, not average of all players
                    .mapToInt(Map.Entry::getValue)
                    .average()
                    .orElse(oldScore);
            int calculatedChange = calculateRatingChange(
                    oldScore, opponentAverage, player.outcome());
            int newScore = Math.max(0, oldScore + calculatedChange);
            int actualChange = newScore - oldScore;

            playerRoundResultRepository.save(PlayerRoundResult.builder()
                    .roundResult(round)
                    .playerProfile(profiles.get(player.userId()))
                    .outcome(player.outcome())
                    .ratingChange(actualChange)
                    .build());

            PlayerSportStats stats = statsByUser.get(player.userId());
            stats.setScore(newScore);
            switch (player.outcome()) {
                case WIN -> stats.setWins(stats.getWins() + 1);
                case LOSS -> stats.setLosses(stats.getLosses() + 1);
                case DRAW -> stats.setDraws(stats.getDraws() + 1);
            }
            playerProfileService.saveSportStats(stats);

            kafkaProducerService.sendGameReplies(
                    new GameReply(
                            dto.activityRequestId(),
                            "CLOSE_REQUEST_RESOLVED",
                            true,
                            null
                    )
            );
        }
        return round.getId();
    }

    @Deprecated(forRemoval = true)
    private void validateResult(SubmitRoundResultDto dto) {
        if (dto == null || dto.activityRequestId() == null) {
            throw new IllegalArgumentException("Activity request ID is required");
        }
        if (dto.sportId() == null || dto.sportId() < 1) {
            throw new IllegalArgumentException("Sport ID must be positive");
        }
        if (dto.roundNumber() < 1) {
            throw new IllegalArgumentException("Round number must be positive");
        }
        if (dto.players() == null || dto.players().isEmpty()) {
            throw new IllegalArgumentException("At least one player result is required");
        }
        Set<UUID> playerIds = new HashSet<>();
        for (SubmitRoundResultDto.PlayerResultEntry player : dto.players()) {
            if (player == null || player.userId() == null || player.outcome() == null) {
                throw new IllegalArgumentException("Each player needs a user ID and an outcome");
            }
            if (!playerIds.add(player.userId())) {
                throw new IllegalArgumentException("A player cannot appear twice in one round");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<RoundResultDetailsDto> getRoundResultsByActivityRequestId(UUID activityRequestId) {
        List<RoundResult> rounds = roundResultRepository
                .findAllByActivityRequestIdOrderByRoundNumberAsc(activityRequestId);
        if (rounds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Round results not found for activity request: " + activityRequestId);
        }
        return rounds.stream().map(this::toDetailsDto).toList();
    }

    private RoundResultDetailsDto toDetailsDto(RoundResult round) {
        List<RoundResultDetailsDto.PlayerResultDto> players = playerRoundResultRepository
                .findAllByRoundResult_Id(round.getId()).stream()
                .map(result -> new RoundResultDetailsDto.PlayerResultDto(
                        result.getPlayerProfile().getUserId(),
                        result.getOutcome(), result.getRatingChange()))
                .sorted(Comparator.comparing(RoundResultDetailsDto.PlayerResultDto::userId))
                .toList();
        return new RoundResultDetailsDto(
                round.getId(), round.getActivityRequestId(), round.getRoundNumber(),
                round.getSportId(), round.getStatus(), round.getCreatedAt(), players);
    }

    @Transactional(readOnly = true)
    public CursorPageDto<PlayerRoundHistoryDto> getPlayerMatches(
            UUID userId, Long sportId, String cursor, int limit) {
        return getPlayerHistory(playerProfileService.getProfileId(userId), sportId, cursor, limit);
    }

    @Transactional(readOnly = true)
    public CursorPageDto<PlayerRoundHistoryDto> getPlayerHistory(
            UUID profileId, Long sportId, String cursor, int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Limit must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (sportId != null && sportId < 1) {
            throw new IllegalArgumentException("Sport ID must be positive");
        }
        RoundHistoryCursor decoded = decodeHistoryCursor(cursor, profileId, sportId);
        List<PlayerHistoryRowProjection> rows = playerRoundResultRepository.findHistory(
                profileId, sportId,
                decoded == null ? null : decoded.createdAt(),
                decoded == null ? null : decoded.roundResultId(),
                limit + 1);
        boolean hasNext = rows.size() > limit;
        List<PlayerHistoryRowProjection> pageRows = hasNext ? rows.subList(0, limit) : rows;
        List<PlayerRoundHistoryDto> items = pageRows.stream().map(row ->
                new PlayerRoundHistoryDto(
                        row.getRoundResultId(), row.getActivityRequestId(), row.getRoundNumber(),
                        row.getSportId(), GameOutcome.valueOf(row.getOutcome()),
                        row.getRatingChange(), row.getCreatedAt())).toList();
        String nextCursor = hasNext ? cursorService.encode(new RoundHistoryCursor(
                HISTORY_CURSOR_TYPE, profileId, sportId,
                pageRows.getLast().getCreatedAt(), pageRows.getLast().getRoundResultId())) : null;
        return new CursorPageDto<>(items, nextCursor, hasNext);
    }

    private RoundHistoryCursor decodeHistoryCursor(String cursor, UUID profileId, Long sportId) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        RoundHistoryCursor decoded = cursorService.decode(cursor, RoundHistoryCursor.class);
        if (!HISTORY_CURSOR_TYPE.equals(decoded.type())
                || !profileId.equals(decoded.playerProfileId())
                || !Objects.equals(sportId, decoded.sportId())
                || decoded.createdAt() == null || decoded.roundResultId() == null) {
            throw new InvalidCursorException("Cursor does not belong to round history");
        }
        return decoded;
    }

    /** Placeholder until the team approves the per-sport rating formula. */
    // TODO: fix in the future
    private int calculateRatingChange(int playerScore, double opponentAverageScore, GameOutcome outcome) {
        int k = 0;
        switch (outcome) {
            case WIN -> k = 1;
            case LOSS -> k = -1;
        }

        return (int) (opponentAverageScore / playerScore * 50) * k;
    }
}
