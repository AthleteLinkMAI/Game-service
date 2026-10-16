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
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.LeaderboardEntryDto;
import team.anonyms.athletelink.gameservice.services.LeaderboardService;

@RestController
@RequestMapping("/leaderboards")
@Tag(name = "Leaderboards")
public class LeaderboardController {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardController.class);

    private final LeaderboardService leaderboardService;

    public LeaderboardController(
            LeaderboardService leaderboardService
    ) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/{sportId}")
    @Operation(summary = "Get sport leaderboard")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid sport ID, cursor or limit")
    })
    public ResponseEntity<CursorPageDto<LeaderboardEntryDto>> getLeaderboard(
            @PathVariable("sportId") Long sportId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/leaderboards/{sportId}")
                .log();
        return ResponseEntity.ok(
                leaderboardService.getLeaderboard(
                        sportId,
                        cursor,
                        limit
                )
        );
    }
}
