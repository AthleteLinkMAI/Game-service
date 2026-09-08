package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "An achievement earned by a player")
public record PlayerAchievementDto(

        @Schema(description = "Achievement code", example = "FIRST_WIN")
        String code,

        @Schema(description = "Display name of the achievement", example = "Первая победа")
        String name,

        @Schema(description = "Achievement description", example = "Выиграйте свой первый матч")
        String description,

        @Schema(description = "URL to the achievement icon")
        String iconUrl,

        @Schema(description = "Timestamp when the player earned this achievement", example = "2026-09-04T18:00:00Z")
        Instant earnedAt
) {}
