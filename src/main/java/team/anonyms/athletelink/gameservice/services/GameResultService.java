package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.GameResultDetailsDto;
import team.anonyms.athletelink.gameservice.dto.PlayerGameHistoryDto;
import team.anonyms.athletelink.gameservice.dto.SubmitGameResultDto;
import team.anonyms.athletelink.gameservice.entities.GameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerGameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;
import team.anonyms.athletelink.gameservice.repositories.GameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerGameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing game results, including submitting match results,
 * computing ELO ratings, awarding points, and retrieving game history.
 *
 * Rating system: simplified ELO with K-factor = 32, starting rating = 1000.
 * Points system: +25 for win, +10 for draw, +5 for loss (participation).
 */
@Service
public class GameResultService {

    private static final int K_FACTOR = 32;
    private static final int POINTS_WIN = 25;
    private static final int POINTS_DRAW = 10;
    private static final int POINTS_LOSS = 5;

    private final GameResultRepository gameResultRepository;
    private final PlayerGameResultRepository playerGameResultRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerProfileService playerProfileService;
    private final AchievementService achievementService;

    public GameResultService(
        GameResultRepository gameResultRepository,
        PlayerGameResultRepository playerGameResultRepository,
        PlayerProfileRepository playerProfileRepository,
        PlayerProfileService playerProfileService,
        AchievementService achievementService
    ) {
        this.gameResultRepository = gameResultRepository;
        this.playerGameResultRepository = playerGameResultRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.playerProfileService = playerProfileService;
        this.achievementService = achievementService;
    }

    /**
     * Submits a game result for a completed match.
     *
     * Business rules:
     * - A game result for the same activity request cannot be submitted twice.
     * - At least one player result must be provided.
     * - Player profiles are auto-created if they don't exist yet.
     * - ELO rating is computed based on average opponent rating.
     * - Points are awarded based on outcome (win/draw/loss).
     * - Achievements are checked and granted after stats update.
     *
     * @param dto Data for the game result submission
     * @param reporterId ID of the user submitting the result (from X-User-Id header)
     * @return The ID of the created game result
     * @throws IllegalArgumentException if a result for this activity request already exists
     */
    @Transactional
    public UUID submitGameResult(SubmitGameResultDto dto, UUID reporterId) {

        gameResultRepository.findByActivityRequestId(dto.activityRequestId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Game result for this activity request has already been submitted");
                });

        GameResult gameResult = GameResult.builder()
                .activityRequestId(dto.activityRequestId())
                .sportId(dto.sportId())
                .status(GameResultStatus.CONFIRMED)
                .reportedBy(reporterId)
                .playedAt(dto.playedAt())
                .build();

        gameResultRepository.save(gameResult);

        double averageRating = dto.players().stream()
                .mapToInt(p -> playerProfileService.getOrCreateProfile(p.userId()).getRating())
                .average()
                .orElse(1000);

        for (SubmitGameResultDto.PlayerResultEntry entry : dto.players()) {
            PlayerProfile profile = playerProfileService.getOrCreateProfile(entry.userId());

            int ratingChange = calculateRatingChange(profile.getRating(), averageRating, entry.outcome());
            int pointsEarned = calculatePoints(entry.outcome());

            PlayerGameResult playerGameResult = PlayerGameResult.builder()
                    .gameResult(gameResult)
                    .playerProfile(profile)
                    .score(entry.score())
                    .outcome(entry.outcome())
                    .ratingChange(ratingChange)
                    .pointsEarned(pointsEarned)
                    .build();

            playerGameResultRepository.save(playerGameResult);

            profile.setRating(Math.max(0, profile.getRating() + ratingChange));
            profile.setTotalPoints(profile.getTotalPoints() + pointsEarned);
            profile.setGamesPlayed(profile.getGamesPlayed() + 1);

            switch (entry.outcome()) {
                case WIN -> profile.setWins(profile.getWins() + 1);
                case LOSS -> profile.setLosses(profile.getLosses() + 1);
                case DRAW -> profile.setDraws(profile.getDraws() + 1);
            }

            playerProfileRepository.save(profile);

            achievementService.checkAndGrantAchievements(profile);
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
                        pr.getScore(),
                        pr.getOutcome(),
                        pr.getRatingChange(),
                        pr.getPointsEarned()
                ))
                .toList();

        return new GameResultDetailsDto(
                gameResult.getId(),
                gameResult.getActivityRequestId(),
                gameResult.getSportId(),
                gameResult.getStatus(),
                gameResult.getPlayedAt(),
                gameResult.getReportedBy(),
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
                .findAllByPlayerProfileIdOrderByGameResult_PlayedAtDesc(playerProfileId)
                .stream()
                .map(pgr -> new PlayerGameHistoryDto(
                        pgr.getGameResult().getId(),
                        pgr.getGameResult().getSportId(),
                        pgr.getScore(),
                        pgr.getOutcome(),
                        pgr.getRatingChange(),
                        pgr.getPointsEarned(),
                        pgr.getGameResult().getPlayedAt()
                ))
                .toList();
    }

    /**
     * Calculates ELO rating change based on the simplified ELO formula.
     *
     * Expected score: E = 1 / (1 + 10^((opponentRating - playerRating) / 400))
     * Rating change: K * (actualScore - expectedScore)
     *
     * @param playerRating Current rating of the player
     * @param opponentRating Average rating of opponents
     * @param outcome The match outcome (WIN/LOSS/DRAW)
     * @return The rating change (can be negative)
     */
    private int calculateRatingChange(int playerRating, double opponentRating, GameOutcome outcome) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentRating - playerRating) / 400.0));

        double actualScore = switch (outcome) {
            case WIN -> 1.0;
            case DRAW -> 0.5;
            case LOSS -> 0.0;
        };

        return (int) Math.round(K_FACTOR * (actualScore - expectedScore));
    }

    /**
     * Calculates points earned based on match outcome.
     *
     * @param outcome The match outcome
     * @return Points earned: 25 for win, 10 for draw, 5 for loss
     */
    private int calculatePoints(GameOutcome outcome) {
        return switch (outcome) {
            case WIN -> POINTS_WIN;
            case DRAW -> POINTS_DRAW;
            case LOSS -> POINTS_LOSS;
        };
    }
}
