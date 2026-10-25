package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistics for a sport other than the player's highest-scoring sport")
public record OtherSportDto(
        Long sportId,
        int score,
        @Schema(description = "Score required for the next rank; null at the highest rank")
        Integer nextRankScore,
        RankDto rank
) {
}
