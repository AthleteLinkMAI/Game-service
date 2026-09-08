package team.anonyms.athletelink.gameservice.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team.anonyms.athletelink.gameservice.dto.LeaderboardEntryDto;
import team.anonyms.athletelink.gameservice.dto.PlayerAchievementDto;
import team.anonyms.athletelink.gameservice.dto.PlayerGameHistoryDto;
import team.anonyms.athletelink.gameservice.dto.PlayerProfileDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.services.AchievementService;
import team.anonyms.athletelink.gameservice.services.GameResultService;
import team.anonyms.athletelink.gameservice.services.PlayerProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Player profiles & leaderboard", description = "API for viewing player profiles, match history, achievements, and leaderboard")
public class PlayerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileController.class);

    private final PlayerProfileService playerProfileService;
    private final GameResultService gameResultService;
    private final AchievementService achievementService;
    private final PlayerProfileRepository playerProfileRepository;

    public PlayerProfileController(
        PlayerProfileService playerProfileService,
        GameResultService gameResultService,
        AchievementService achievementService,
        PlayerProfileRepository playerProfileRepository
    ) {
        this.playerProfileService = playerProfileService;
        this.gameResultService = gameResultService;
        this.achievementService = achievementService;
        this.playerProfileRepository = playerProfileRepository;
    }

    @GetMapping("/profiles/{userId}")
    @Operation(
        summary = "Get player profile",
        description = "Retrieves a player's game profile with aggregated statistics: rating, total points, wins, losses, and draws."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<PlayerProfileDto> getProfile(
            @Parameter(description = "User UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("userId") UUID userId
    ) {
        logger.info("Fetching profile for user {}", userId);
        PlayerProfileDto profile = playerProfileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profiles/{userId}/history")
    @Operation(
        summary = "Get match history",
        description = "Retrieves a player's match history sorted by most recent first, including scores, outcomes, and rating changes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Match history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<List<PlayerGameHistoryDto>> getPlayerHistory(
            @Parameter(description = "User UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("userId") UUID userId
    ) {
        logger.info("Fetching match history for user {}", userId);

        PlayerProfile profile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Player profile not found"));

        List<PlayerGameHistoryDto> history = gameResultService.getPlayerHistory(profile.getId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/profiles/{userId}/achievements")
    @Operation(
        summary = "Get player achievements",
        description = "Retrieves all achievements earned by a specific player."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Achievements retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<List<PlayerAchievementDto>> getPlayerAchievements(
            @Parameter(description = "User UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("userId") UUID userId
    ) {
        logger.info("Fetching achievements for user {}", userId);

        PlayerProfile profile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Player profile not found"));

        List<PlayerAchievementDto> achievements = achievementService.getPlayerAchievements(profile.getId());
        return ResponseEntity.ok(achievements);
    }

    @GetMapping("/leaderboard")
    @Operation(
        summary = "Get leaderboard",
        description = "Returns the top 50 players sorted by ELO rating in descending order."
    )
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard() {
        logger.info("Fetching leaderboard");
        List<LeaderboardEntryDto> leaderboard = playerProfileService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }
}
