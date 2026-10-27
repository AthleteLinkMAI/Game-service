package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configured score threshold for a rank tier")
public record RankTierDto(

        @Schema(description = "Rank name", example = "GOLD")
        String name,

        @Schema(description = "Tier number from 1 to 3", example = "1")
        int tier,

        @Schema(description = "Minimum score included in this rank", example = "1200")
        int scoreMin,

        @Schema(description = "Maximum score included in this rank", example = "1399")
        int scoreMax
) {
}
