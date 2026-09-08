package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A single entry in the leaderboard")
public record LeaderboardEntryDto(

        @Schema(description = "Position in the leaderboard", example = "1")
        Integer rank,

        @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,

        @Schema(description = "Current ELO rating", example = "1450")
        Integer rating,

        @Schema(description = "Total games played", example = "42")
        Integer gamesPlayed,

        @Schema(description = "Total wins", example = "30")
        Integer wins
) {}
