package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Player rank calculated from score")
public record RankDto(

        @Schema(description = "Rank name", example = "GOLD")
        String name,

        @Schema(description = "Rank tier", example = "1")
        int tier,

        @Schema(description = "Minimum score", example = "1200")
        int scoreMin,

        @Schema(description = "Maximum score", example = "1399")
        int scoreMax
) {
}