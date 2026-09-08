package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Player profile with aggregated game statistics")
public record PlayerProfileDto(

        @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,

        @Schema(description = "Total accumulated points", example = "450")
        Integer totalPoints,

        @Schema(description = "Current ELO rating", example = "1120")
        Integer rating,

        @Schema(description = "Total games played", example = "25")
        Integer gamesPlayed,

        @Schema(description = "Total wins", example = "15")
        Integer wins,

        @Schema(description = "Total losses", example = "8")
        Integer losses,

        @Schema(description = "Total draws", example = "2")
        Integer draws,

        @Schema(description = "Profile creation date", example = "2026-01-15T10:00:00Z")
        Instant createdAt
) {}
