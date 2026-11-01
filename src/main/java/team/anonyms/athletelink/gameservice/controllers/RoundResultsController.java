package team.anonyms.athletelink.gameservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.gameservice.dto.RequestRoundResultDto;
import team.anonyms.athletelink.gameservice.exceptions.GlobalExceptionHandler.ErrorResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/requests/{requestId}/rounds")
@Tag(name = "Request rounds (draft)", description = "Planned Game-owned API for recording and reading rounds after Request Service starts a request")
public class RoundResultsController {

    @PostMapping("/{roundNumber}/result")
    @Operation(
            summary = "Record a round result",
            description = "Draft contract. The organizer submits two non-empty, equally sized and disjoint teams from the accepted start roster. "
                    + "The round number starts at 1 and cannot exceed the request's numberOfRounds. Game records and scores the round. "
                    + "After the final round is recorded, Game marks the request COMPLETED and publishes REQUEST_STATUS_CHANGED to Request Service; "
                    + "there is no separate completion request. This handler currently has no implementation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Round recorded and scored; response also shows the Game-owned request status",
                    content = @Content(schema = @Schema(implementation = RequestRoundResultDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed UUID, invalid round number, duplicate or overlapping players, unequal team sizes, or a player outside the accepted roster"),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Started request not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Request is not active or this round already has a result",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RequestRoundResultDto> recordRoundResult(
            @Parameter(description = "UUID of the request previously started by Request Service", required = true,
                    example = "22222222-2222-2222-2222-222222222222")
            @PathVariable("requestId") UUID requestId,
            @Parameter(description = "One-based round number", required = true, example = "1")
            @Positive @PathVariable("roundNumber") Integer roundNumber,
            @Parameter(description = "Organizer UUID supplied by the API Gateway; this header is not a substitute for authentication", required = true,
                    example = "44444444-4444-4444-4444-444444444444")
            @RequestHeader("X-User-Id") UUID organizerId,
            @Valid @RequestBody RecordRoundResultRequestDto result
    ) {
        return null;
    }

    @GetMapping
    @Operation(
            summary = "List recorded rounds",
            description = "Draft contract. Returns the request's recorded rounds in ascending round-number order. "
                    + "An active request with no recorded rounds returns an empty list. This handler currently has no implementation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recorded rounds, possibly empty",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestRoundResultDto.class)))),
            @ApiResponse(responseCode = "400", description = "Malformed request UUID"),
            @ApiResponse(responseCode = "404", description = "Started request not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RequestRoundResultDto>> getRoundResults(
            @Parameter(description = "UUID of the request previously started by Request Service", required = true,
                    example = "22222222-2222-2222-2222-222222222222")
            @PathVariable("requestId") UUID requestId
    ) {
        return null;
    }
}
