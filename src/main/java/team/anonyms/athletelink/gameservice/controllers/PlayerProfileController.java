package team.anonyms.athletelink.gameservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.PlayerGameHistoryDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSportDetailsDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSummaryDto;
import team.anonyms.athletelink.gameservice.services.GameResultService;
import team.anonyms.athletelink.gameservice.services.PlayerProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name = "Player profiles", description = "API for viewing player profiles and match history")
public class PlayerProfileController {

    private final PlayerProfileService playerProfileService;
    private final GameResultService gameResultService;

    public PlayerProfileController(
            PlayerProfileService playerProfileService,
            GameResultService gameResultService
    ) {
        this.playerProfileService = playerProfileService;
        this.gameResultService = gameResultService;
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
        return ResponseEntity.ok(
                playerProfileService.getSummary(userId)
        );
    }



    @GetMapping("/{userId}/sports/{sportId}")
    @Operation(summary = "Get player statistics for one sport")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sport statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Player, sport or statistics not found")
    })
    public ResponseEntity<PlayerSportDetailsDto> getSportDetails(
            @PathVariable("userId") UUID userId,
            @PathVariable("sportId") Long sportId
    ) {
        return ResponseEntity.ok(
                playerProfileService.getSportDetails(
                        userId,
                        sportId
                )
        );
    }

    @GetMapping("/{userId}/matches")
    @Operation(summary = "Get paginated player match history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match history retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cursor or limit"),
            @ApiResponse(responseCode = "404", description = "Player profile or sport not found")
    })
    public ResponseEntity<CursorPageDto<PlayerGameHistoryDto>> getMatches(
            @PathVariable("userId") UUID userId,
            @RequestParam(name = "sportId", required = false) Long sportId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(
                gameResultService.getPlayerMatches(
                        userId,
                        sportId,
                        cursor,
                        limit
                )
        );
    }
}
