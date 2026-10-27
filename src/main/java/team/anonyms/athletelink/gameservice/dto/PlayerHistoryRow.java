package team.anonyms.athletelink.gameservice.dto;

import java.time.Instant;
import java.util.UUID;


public record PlayerHistoryRow(
        UUID roundResultId,
        UUID activityRequestId,
        Integer roundNumber,
        Long sportId,
        String outcome,
        Integer ratingChange,
        Instant createdAt
) {
}
