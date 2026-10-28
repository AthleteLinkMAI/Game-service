package team.anonyms.athletelink.gameservice.dto;

import java.util.UUID;


public record LeaderboardRow(
        UUID userId,
        Integer score,
        Integer wins,
        Integer losses,
        Integer draws,
        Long place
) {
}
