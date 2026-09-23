package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Player statistics for one sport")
public record PlayerSportStatsDto(

        @Schema(description = "Sport identifier", example = "1")
        Long sportId,

        @Schema(description = "Wins in this sport", example = "12")
        Integer wins,

        @Schema(description = "Losses in this sport", example = "4")
        Integer losses,

        @Schema(description = "Draws in this sport", example = "1")
        Integer draws,

        @Schema(description = "Current score in this sport", example = "1200")
        Integer score,

        @Schema(description = "Rank calculated from score")
        RankDto rank
) {

    public record RankDto(
            UUID id,
            String name,
            Integer tier,
            Integer scoreMin,
            Integer scoreMax
    ) {
    }
}
