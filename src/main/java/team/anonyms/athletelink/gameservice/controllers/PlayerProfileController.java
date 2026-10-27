package team.anonyms.athletelink.gameservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.BestSportDto;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.LastGameDto;
import team.anonyms.athletelink.gameservice.dto.OtherSportDto;
import team.anonyms.athletelink.gameservice.dto.PlayerRoundHistoryDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSportDetailsDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSummaryDto;
import team.anonyms.athletelink.gameservice.services.RoundResultService;
import team.anonyms.athletelink.gameservice.services.PlayerProfileService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/players")
@Tag(name = "Player profiles", description = "API for viewing player profiles and match history")
public class PlayerProfileController {

    private static final Logger log = LoggerFactory.getLogger(PlayerProfileController.class);

    private final PlayerProfileService playerProfileService;
    private final RoundResultService roundResultService;

    public PlayerProfileController(
            PlayerProfileService playerProfileService,
            RoundResultService roundResultService
    ) {
        this.playerProfileService = playerProfileService;
        this.roundResultService = roundResultService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get player summary")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<PlayerSummaryDto> getSummary(
            @PathVariable("userId") UUID userId
    ) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/players/{userId}")
                .log();
        return ResponseEntity.ok(
                playerProfileService.getSummary(userId)
        );
    }



    @GetMapping("/{userId}/sports/{sportId}")
    @Operation(summary = "Get player statistics for one sport")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sport statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Player or sport statistics not found")
    })
    public ResponseEntity<PlayerSportDetailsDto> getSportDetails(
            @PathVariable("userId") UUID userId,
            @PathVariable("sportId") Long sportId
    ) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/players/{userId}/sports/{sportId}")
                .log();
        return ResponseEntity.ok(
                playerProfileService.getSportDetails(
                        userId,
                        sportId
                )
        );
    }

    @GetMapping("/{userId}/best-sport")
    @Operation(summary = "Get the player's highest-scoring sport")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Best sport retrieved successfully"),
            @ApiResponse(responseCode = "204", description = "Player has no sport statistics"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<BestSportDto> getBestSport(@PathVariable("userId") UUID userId) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/players/{userId}/best-sport")
                .log();
        return playerProfileService.getBestSport(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{userId}/other-sports")
    @Operation(summary = "Get all player sports except the highest-scoring one")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Other sports retrieved, possibly empty"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<List<OtherSportDto>> getOtherSports(@PathVariable("userId") UUID userId) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/players/{userId}/other-sports")
                .log();
        return ResponseEntity.ok(playerProfileService.getOtherSports(userId));
    }

    @GetMapping("/{userId}/last-game")
    @Operation(summary = "Get the most recently recorded round played by the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Last round retrieved successfully"),
            @ApiResponse(responseCode = "204", description = "Player has no recorded rounds"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<LastGameDto> getLastGame(@PathVariable("userId") UUID userId) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/players/{userId}/last-game")
                .log();
        return roundResultService.getLastGame(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{userId}/matches")
    @Operation(summary = "Get paginated player round history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match history retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cursor or limit"),
            @ApiResponse(responseCode = "404", description = "Player profile not found")
    })
    public ResponseEntity<CursorPageDto<PlayerRoundHistoryDto>> getMatches(
            @PathVariable("userId") UUID userId,
            @RequestParam(name = "sportId", required = false) Long sportId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/players/{userId}/matches")
                .log();
        return ResponseEntity.ok(
                roundResultService.getPlayerMatches(
                        userId,
                        sportId,
                        cursor,
                        limit
                )
        );
    }
}
