package team.anonyms.athletelink.gameservice.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team.anonyms.athletelink.gameservice.dto.GameResultDetailsDto;
import team.anonyms.athletelink.gameservice.dto.SubmitGameResultDto;
import team.anonyms.athletelink.gameservice.services.GameResultService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/game-results")
@Tag(name = "Game results", description = "API for submitting and viewing game results")
public class GameResultController {

    private static final Logger logger = LoggerFactory.getLogger(GameResultController.class);

    private final GameResultService gameResultService;

    public GameResultController(GameResultService gameResultService) {
        this.gameResultService = gameResultService;
    }

    @PostMapping
    @Operation(
        summary = "Submit game result",
        description = "Submits the result of a completed match and updates each participant's statistics for the selected sport."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Game result submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or result already submitted for this activity request")
    })
    public ResponseEntity<Map<String, UUID>> submitGameResult(
            @RequestBody @Valid SubmitGameResultDto dto,
            @Parameter(description = "User ID from API Gateway", hidden = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.info("User {} is submitting game result for activity request {}", userId, dto.activityRequestId());

        UUID gameResultId = gameResultService.submitGameResult(dto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("gameResultId", gameResultId));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get game result details",
        description = "Retrieves detailed information about a specific game result, including individual player outcomes and rating changes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game result details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Game result not found")
    })
    public ResponseEntity<GameResultDetailsDto> getGameResultDetails(
            @Parameter(description = "Game result UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID gameResultId
    ) {
        logger.info("Fetching details for game result {}", gameResultId);
        GameResultDetailsDto details = gameResultService.getGameResultDetails(gameResultId);
        return ResponseEntity.ok(details);
    }
}
