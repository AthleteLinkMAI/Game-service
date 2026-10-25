package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Player's highest-scoring sport")
public record BestSportDto(
        Long sportId,
        int score,
        @Schema(description = "Score required for the next rank; null at the highest rank")
        Integer nextRankScore,
        @Schema(description = "Place among players of this sport; equal scores share a place")
        long leaderboardPlace,
        int wins,
        @Schema(description = "Wins divided by all rounds in this sport, as a percentage")
        double winRatePercent,
        RankDto rank
) {
}
