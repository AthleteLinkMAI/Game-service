package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Data for submitting a game result after a completed match")
public record SubmitGameResultDto(

        @Schema(description = "ID of the activity request from core-service", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "Activity request ID is required")
        UUID activityRequestId,

        @Schema(description = "ID of the sport (from /sports in core-service)", example = "1")
        @NotNull(message = "Sport ID is required")
        Long sportId,

        @Schema(description = "When the match was played", example = "2026-09-04T18:00:00Z")
        @NotNull(message = "Played at date is required")
        Instant playedAt,

        @Schema(description = "Individual results for each participant")
        @NotEmpty(message = "At least one player result is required")
        List<@Valid PlayerResultEntry> players
) {

    @Schema(description = "Individual player result entry")
    public record PlayerResultEntry(

            @Schema(description = "User ID of the player", example = "123e4567-e89b-12d3-a456-426614174000")
            @NotNull(message = "User ID is required")
            UUID userId,

            @Schema(description = "Score/goals of the player in this match", example = "3")
            @NotNull(message = "Score is required")
            Integer score,

            @Schema(description = "Outcome for this player", example = "WIN")
            @NotNull(message = "Outcome is required")
            GameOutcome outcome
    ) {}
}
