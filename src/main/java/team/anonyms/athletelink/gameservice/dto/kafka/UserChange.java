package team.anonyms.athletelink.gameservice.dto.kafka;

import team.anonyms.athletelink.gameservice.dto.kafka.enums.UserChangeType;

import java.util.UUID;

public record UserChange(
        UserChangeType userChangeType,
        UUID userId
) {
}
