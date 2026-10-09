package team.anonyms.athletelink.gameservice.dto.kafka;

import java.util.UUID;

public record GameReply(
        UUID requestId,
        String type,
        Boolean isSuccessful,
        String errorMessage
) {
}
