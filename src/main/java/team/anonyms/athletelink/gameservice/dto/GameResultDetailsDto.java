package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed information about a game result")
public record GameResultDetailsDto(

        @Schema(description = "Unique game result ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "ID of the corresponding activity request", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID activityRequestId,

        @Schema(description = "Sport ID", example = "1")
        Long sportId,

        @Schema(description = "Current status of the game result")
        GameResultStatus status,

        @Schema(description = "When the match was played", example = "2026-09-04T18:00:00Z")
        Instant playedAt,

        @Schema(description = "ID of the user who reported the result")
        UUID reportedBy,

        @Schema(description = "Individual player results")
        List<PlayerResultDto> players
) {

    @Schema(description = "Individual player result within a game")
    public record PlayerResultDto(

            @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            UUID userId,

            @Schema(description = "Score in the match", example = "3")
            Integer score,

            @Schema(description = "Match outcome for this player")
            GameOutcome outcome,

            @Schema(description = "Rating change after this match", example = "+15")
            Integer ratingChange,

            @Schema(description = "Points earned for this match", example = "25")
            Integer pointsEarned
    ) {}
}
