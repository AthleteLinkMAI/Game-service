package team.anonyms.athletelink.gameservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.GameResultDetailsDto;
import team.anonyms.athletelink.gameservice.services.GameResultService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/results")
@Tag(name = "Results")
public class GameResultQueryController {

    private final GameResultService gameResultService;

    public GameResultQueryController(GameResultService gameResultService) {
        this.gameResultService = gameResultService;
    }

    @GetMapping("/by-request/{activityRequestId}")
    @Operation(summary = "Get result by activity request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Game result retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Game result not found")
    })
    public ResponseEntity<GameResultDetailsDto> getByActivityRequest(
            @PathVariable("activityRequestId") UUID activityRequestId
    ) {
        return ResponseEntity.ok(
                gameResultService.getGameResultByActivityRequestId(activityRequestId)
        );
    }
}
