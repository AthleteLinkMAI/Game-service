package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Organizer's decision for one pending join attempt")
public record JoinDecisionRequestDto(
        @Schema(description = "True to approve the player; false to reject and remove the pending participant from Request Service",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "The decision is required")
        Boolean accepted
) {}
