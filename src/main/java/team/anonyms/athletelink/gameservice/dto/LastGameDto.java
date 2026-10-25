package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "The most recently recorded round played by a user")
public record LastGameDto(
        UUID roundResultId,
        UUID activityRequestId,
        int roundNumber,
        Long sportId,
        GameOutcome outcome,
        @Schema(description = "Time the result was recorded, not the date the game was played")
        Instant createdAt,
        @Schema(description = "User IDs of all participants in this round, including the requested player")
        List<UUID> playerIds
) {
}
