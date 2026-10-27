package team.anonyms.athletelink.gameservice.dto.kafka;

import java.util.UUID;

public record GameReply(
        String type,
        UUID requestId,
        Boolean isSuccessful,
        String errorMessage
) {
}
