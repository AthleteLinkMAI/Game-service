package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data transfer object representing an available achievement")
public record AchievementDto(

        @Schema(description = "Unique identifier of the achievement", example = "1")
        Long id,

        @Schema(description = "Achievement code", example = "FIRST_WIN")
        String code,

        @Schema(description = "Display name of the achievement", example = "Первая победа")
        String name,

        @Schema(description = "Description of how to earn the achievement", example = "Выиграйте свой первый матч")
        String description,

        @Schema(description = "URL to the achievement icon", example = "https://api.athletelink.com/static/icons/first_win.png")
        String iconUrl
) {}
