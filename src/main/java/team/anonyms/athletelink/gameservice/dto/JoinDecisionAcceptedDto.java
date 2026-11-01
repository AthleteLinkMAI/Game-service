package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Game accepted the organizer's decision for asynchronous delivery to Request Service. This response does not confirm that Request Service has applied it.")
public record JoinDecisionAcceptedDto(
        @Schema(description = "Original join attempt event ID; the reply uses this same ID", example = "11111111-1111-1111-1111-111111111111")
        UUID eventId,
        @Schema(description = "Activity request UUID", example = "22222222-2222-2222-2222-222222222222")
        UUID requestId,
        @Schema(description = "UUID of the player who requested to join", example = "33333333-3333-3333-3333-333333333333")
        UUID userId,
        @Schema(description = "Decision that will be sent to Request Service", example = "true")
        boolean accepted
) {}
