package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "One round in a player's history")
public record PlayerRoundHistoryDto(
        UUID roundResultId,
        UUID activityRequestId,
        int roundNumber,
        Long sportId,
        GameOutcome outcome,
        Integer ratingChange,
        Instant createdAt
) {}
