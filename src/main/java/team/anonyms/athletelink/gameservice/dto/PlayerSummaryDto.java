package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Aggregated player statistics")
public record PlayerSummaryDto(

        @Schema(
                description = "User ID",
                example = "123e4567-e89b-12d3-a456-426614174000"
        )
        UUID userId,

        @Schema(description = "Aggregated statistics across all sports")
        Summary summary
) {

    public record Summary(

            @Schema(description = "Number of sports played", example = "2")
            long sportsPlayed,

            @Schema(description = "Total number of rounds", example = "12")
            int roundsPlayed,

            @Schema(description = "Total wins", example = "7")
            int wins,

            @Schema(description = "Total losses", example = "4")
            int losses,

            @Schema(description = "Total draws", example = "1")
            int draws
    ) {
    }
}
