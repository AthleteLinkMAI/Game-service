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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.JoinDecisionAcceptedDto;
import team.anonyms.athletelink.gameservice.dto.JoinDecisionRequestDto;
import team.anonyms.athletelink.gameservice.dto.JoinRequestDto;
import team.anonyms.athletelink.gameservice.exceptions.GlobalExceptionHandler.ErrorResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/requests/{requestId}/join-requests")
@Tag(name = "Join approvals (draft)", description = "Planned organizer-facing API for deciding pending Request Service join attempts")
public class JoinRequestsController {

    @GetMapping
    @Operation(
            summary = "List pending join requests",
            description = "Draft contract. Lists unresolved JOIN_REQUESTED events for this activity request and organizer. "
                    + "These events arrive before the request's start snapshot, so an empty list means Game has no pending decisions "
                    + "for this caller and request; it does not prove the request exists. Each eventId identifies one attempt and must be used for its decision. "
                    + "This handler currently has no implementation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending join attempts, possibly empty",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = JoinRequestDto.class)))),
            @ApiResponse(responseCode = "400", description = "Malformed request or caller UUID"),
            @ApiResponse(responseCode = "403", description = "Caller does not match the organizer on a known pending attempt",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<JoinRequestDto>> getPendingJoinRequests(
            @Parameter(description = "Activity request UUID", required = true, example = "22222222-2222-2222-2222-222222222222")
            @PathVariable("requestId") UUID requestId,
            @Parameter(description = "Organizer UUID supplied by the API Gateway; this header is not a substitute for authentication", required = true,
                    example = "44444444-4444-4444-4444-444444444444")
            @RequestHeader("X-User-Id") UUID organizerId
    ) {
        return null;
    }

    @PostMapping("/{eventId}/decision")
    @Operation(
            summary = "Approve or reject one join request",
            description = "Draft contract. Only the organizer may decide. Game sends JOIN_REQUEST_RESOLVED to the shared game.replies Kafka topic "
                    + "using the original join attempt eventId, requestId, userId and the accepted flag. "
                    + "Request Service applies an approval only if this exact attempt is still PENDING and the request is still PLANNED; "
                    + "a rejection removes that pending player. HTTP 202 means the decision was accepted for delivery, not that the player's "
                    + "status has already changed in Request Service. This handler currently has no implementation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Decision accepted for asynchronous delivery to Request Service",
                    content = @Content(schema = @Schema(implementation = JoinDecisionAcceptedDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed UUID or missing accepted flag"),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pending join attempt not found for this request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Join attempt already decided or no longer pending",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<JoinDecisionAcceptedDto> decideJoinRequest(
            @Parameter(description = "Activity request UUID", required = true, example = "22222222-2222-2222-2222-222222222222")
            @PathVariable("requestId") UUID requestId,
            @Parameter(description = "Original JOIN_REQUESTED event UUID; distinguishes repeated attempts by the same user", required = true,
                    example = "11111111-1111-1111-1111-111111111111")
            @PathVariable("eventId") UUID eventId,
            @Parameter(description = "Organizer UUID supplied by the API Gateway; this header is not a substitute for authentication", required = true,
                    example = "44444444-4444-4444-4444-444444444444")
            @RequestHeader("X-User-Id") UUID organizerId,
            @Valid @RequestBody JoinDecisionRequestDto decision
    ) {
        return null;
    }
}
