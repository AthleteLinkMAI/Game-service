package team.anonyms.athletelink.gameservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.CloseRequestDto;
import team.anonyms.athletelink.gameservice.dto.PageDto;
import team.anonyms.athletelink.gameservice.dto.LastGameDto;
import team.anonyms.athletelink.gameservice.dto.PlayerHistoryRow;
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
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.repositories.PlayerRoundResultRepository;
import team.anonyms.athletelink.gameservice.repositories.RoundResultRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoundResultService {
    private static final int MAX_PAGE_SIZE = 100;

    private final RoundResultRepository roundResultRepository;
    private final PlayerRoundResultRepository playerRoundResultRepository;
    private final PlayerProfileService playerProfileService;
    private final KafkaProducerService kafkaProducerService;

    /** One Kafka event is one round; all writes commit or roll back together. */
    @Transactional
    public UUID submitRoundResult(SubmitRoundResultDto dto) {
        validateResult(dto);

        var existing = roundResultRepository.findByActivityRequestIdAndRoundNumber(
                dto.requestId(), dto.roundNumber());
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
                .activityRequestId(dto.requestId())
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
                            "CLOSE_REQUEST_RESOLVED",
                            dto.requestId(),
                            true,
                            null
                    )
            );
        }
        return round.getId();
    }

    public void validateGameForClosing(CloseRequestDto dto) {
        List<RoundResult> roundResults = roundResultRepository.findAllByActivityRequestIdOrderByRoundNumberAsc(dto.requestId());

        if (roundResults.size() != dto.expectedRounds()) {
            kafkaProducerService.sendGameReplies(
                    new GameReply(
                            "CLOSE_REQUEST_UNRESOLVED",
                            dto.requestId(),
                            false,
                            "Insufficient number of rounds"
                    )
            );
        }

        kafkaProducerService.sendGameReplies(new GameReply("CLOSE_REQUEST_RESOLVED", dto.requestId(), true, null));
    }

    @Deprecated(forRemoval = true)
    private void validateResult(SubmitRoundResultDto dto) {
        if (dto == null || dto.requestId() == null) {
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
    public PageDto<PlayerRoundHistoryDto> getPlayerMatches(
            UUID userId, Long sportId, int page, int cntPerPage) {
        return getPlayerHistory(playerProfileService.getProfileId(userId), sportId, page, cntPerPage);
    }

    @Transactional(readOnly = true)
    public Optional<LastGameDto> getLastGame(UUID userId) {
        UUID profileId = playerProfileService.getProfileId(userId);
        List<PlayerHistoryRow> history = playerRoundResultRepository.findHistory(
                profileId, null, 1, 0);
        if (history.isEmpty()) {
            return Optional.empty();
        }

        PlayerHistoryRow last = history.getFirst();
        List<UUID> playerIds = playerRoundResultRepository
                .findAllByRoundResult_Id(last.roundResultId()).stream()
                .map(result -> result.getPlayerProfile().getUserId())
                .sorted()
                .toList();
        return Optional.of(new LastGameDto(
                last.roundResultId(), last.activityRequestId(), last.roundNumber(),
                last.sportId(), GameOutcome.valueOf(last.outcome()), last.createdAt(),
                playerIds
        ));
    }

    @Transactional(readOnly = true)
    public PageDto<PlayerRoundHistoryDto> getPlayerHistory(
            UUID profileId, Long sportId, int page, int cntPerPage) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be at least 1");
        }
        if (cntPerPage < 1 || cntPerPage > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Count per page must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (sportId != null && sportId < 1) {
            throw new IllegalArgumentException("Sport ID must be positive");
        }
        long offset = (long) (page - 1) * cntPerPage;
        List<PlayerHistoryRow> rows = playerRoundResultRepository.findHistory(
                profileId, sportId, cntPerPage, offset);
        List<PlayerRoundHistoryDto> items = rows.stream().map(row ->
                new PlayerRoundHistoryDto(
                        row.roundResultId(), row.activityRequestId(), row.roundNumber(),
                        row.sportId(), GameOutcome.valueOf(row.outcome()),
                        row.ratingChange(), row.createdAt())).toList();
        return new PageDto<>(items, page, cntPerPage);
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
