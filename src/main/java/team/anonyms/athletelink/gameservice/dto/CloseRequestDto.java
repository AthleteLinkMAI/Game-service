package team.anonyms.athletelink.gameservice.dto;

import java.util.UUID;

public record CloseRequestDto(
        String type,
        UUID requestId,
        Integer expectedRounds
) {
}
