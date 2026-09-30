package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Result of one round in an activity request")
public record RoundResultDetailsDto(
        UUID id,
        UUID activityRequestId,
        int roundNumber,
        Long sportId,
        GameResultStatus status,
        Instant createdAt,
        List<PlayerResultDto> players
) {
    public record PlayerResultDto(
            UUID userId,
            GameOutcome outcome,
            Integer ratingChange
    ) {}
}
