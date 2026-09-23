package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Player profile with aggregated game statistics")
public record PlayerProfileDto(

        @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,

        @Schema(description = "Statistics grouped by sport")
        List<PlayerSportStatsDto> sportStats
) {}
