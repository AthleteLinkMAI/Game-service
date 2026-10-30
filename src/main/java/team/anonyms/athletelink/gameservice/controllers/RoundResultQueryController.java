package team.anonyms.athletelink.gameservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.RoundResultDetailsDto;
import team.anonyms.athletelink.gameservice.services.RoundResultService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/results")
@Tag(name = "Results")
public class RoundResultQueryController {

    private final RoundResultService roundResultService;

    public RoundResultQueryController(RoundResultService roundResultService) {
        this.roundResultService = roundResultService;
    }

    @GetMapping("/by-request/{activityRequestId}")
    @Operation(summary = "Get all round results for an activity request in round-number order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Round results retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No round results found")
    })
    public ResponseEntity<List<RoundResultDetailsDto>> getByActivityRequest(
            @PathVariable("activityRequestId") UUID activityRequestId
    ) {
        log.atInfo()
                .setMessage("API endpoint is called")
                .addKeyValue("API endpoint", "GET /api/v1/results/by-request/{activityRequestId}")
                .log();
        return ResponseEntity.ok(
                roundResultService.getRoundResultsByActivityRequestId(activityRequestId)
        );
    }
}
