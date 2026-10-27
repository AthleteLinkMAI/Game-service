package team.anonyms.athletelink.gameservice.pagination;

import java.time.Instant;
import java.util.UUID;

public record RoundHistoryCursor(
        String type,
        UUID playerProfileId,
        Long sportId,
        Instant createdAt,
        UUID roundResultId
) {}
