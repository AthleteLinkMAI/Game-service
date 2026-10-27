package team.anonyms.athletelink.gameservice.pagination;

import java.util.UUID;

public record LeaderboardCursor(
        String type,
        Long sportId,
        int score,
        UUID playerProfileId
) {
}
