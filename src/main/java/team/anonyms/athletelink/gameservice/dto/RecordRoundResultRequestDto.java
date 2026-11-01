package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Organizer's result for one round. Both teams must contain accepted players from the start roster, have equal nonzero size, contain no duplicates, and have no player in both teams. These cross-field rules describe the planned Game validation.")
public record RecordRoundResultRequestDto(

        @Schema(description = "User UUIDs of the winning team; no duplicate players", example = "[\"33333333-3333-3333-3333-333333333333\"]")
        @NotNull(message = "Winners are required")
        @NotEmpty(message = "Winners must not be empty")
        List<@NotNull UUID> winners,

        @Schema(description = "User UUIDs of the losing team; no duplicate players", example = "[\"55555555-5555-5555-5555-555555555555\"]")
        @NotNull(message = "Losers are required")
        @NotEmpty(message = "Losers must not be empty")
        List<@NotNull UUID> losers
) {}
