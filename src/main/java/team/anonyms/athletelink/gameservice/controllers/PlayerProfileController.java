package team.anonyms.athletelink.gameservice.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team.anonyms.athletelink.gameservice.dto.PlayerGameHistoryDto;
import team.anonyms.athletelink.gameservice.dto.PlayerProfileDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.services.GameResultService;
import team.anonyms.athletelink.gameservice.services.PlayerProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;
import java.util.List;

@RestController
@Tag(name = "Player profiles", description = "API for viewing player profiles and match history")
public class PlayerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileController.class);

    private final PlayerProfileService playerProfileService;
    private final GameResultService gameResultService;
    private final PlayerProfileRepository playerProfileRepository;

    public PlayerProfileController(
        PlayerProfileService playerProfileService,
        GameResultService gameResultService,
        PlayerProfileRepository playerProfileRepository
    ) {
        this.playerProfileService = playerProfileService;
        this.gameResultService = gameResultService;
        this.playerProfileRepository = playerProfileRepository;
    }

    @GetMapping("/profiles/{userId}")
    @Operation(
        summary = "Get player profile",
        description = "Retrieves a player's statistics grouped by sport."
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
        description = "Retrieves a player's match history sorted by most recent first, including outcomes and rating changes."
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

}
