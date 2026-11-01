package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Pending join attempt forwarded by Request Service. The event ID identifies this specific attempt, not just the user; a later attempt has a different ID.")
public record JoinRequestDto(
        @Schema(description = "Original JOIN_REQUESTED event ID; used to correlate the organizer's decision", example = "11111111-1111-1111-1111-111111111111")
        UUID eventId,
        @Schema(description = "Activity request UUID", example = "22222222-2222-2222-2222-222222222222")
        UUID requestId,
        @Schema(description = "UUID of the user asking to join", example = "33333333-3333-3333-3333-333333333333")
        UUID userId,
        @Schema(description = "UUID of the organizer who can decide", example = "44444444-4444-4444-4444-444444444444")
        UUID organizerId
) {}
