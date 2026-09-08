package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A single entry in a player's match history")
public record PlayerGameHistoryDto(

        @Schema(description = "Game result ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID gameResultId,

        @Schema(description = "Sport ID", example = "1")
        Long sportId,

        @Schema(description = "Score in the match", example = "3")
        Integer score,

        @Schema(description = "Outcome for this player")
        GameOutcome outcome,

        @Schema(description = "Rating change after this match", example = "+15")
        Integer ratingChange,

        @Schema(description = "Points earned for this match", example = "25")
        Integer pointsEarned,

        @Schema(description = "When the match was played", example = "2026-09-04T18:00:00Z")
        Instant playedAt
) {}
