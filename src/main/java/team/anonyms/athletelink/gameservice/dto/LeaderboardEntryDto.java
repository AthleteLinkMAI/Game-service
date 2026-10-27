package team.anonyms.athletelink.gameservice.dto;

import java.util.UUID;

public record LeaderboardEntryDto(
        long place,
        UUID userId,
        int score,
        int wins,
        int losses,
        int draws,
        RankDto rank
) {
}