package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Player statistics for one sport")
public record PlayerSportDetailsDto(

        @Schema(description = "User ID")
        UUID userId,

        @Schema(description = "Sport ID", example = "1")
        Long sportId,

        @Schema(description = "Sport name", example = "Football")
        String sportName,

        @Schema(description = "Games played in this sport", example = "8")
        int gamesPlayed,

        @Schema(description = "Wins in this sport", example = "5")
        int wins,

        @Schema(description = "Losses in this sport", example = "2")
        int losses,

        @Schema(description = "Draws in this sport", example = "1")
        int draws,

        @Schema(description = "Current score", example = "1200")
        int score,

        @Schema(description = "Current leaderboard place", example = "7")
        long leaderboardPlace,

        @Schema(description = "Rank calculated from score")
        RankDto rank
) {
}