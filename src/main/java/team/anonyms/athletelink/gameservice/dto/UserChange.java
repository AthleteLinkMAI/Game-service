package team.anonyms.athletelink.gameservice.dto;

import java.util.UUID;

/** Wire format published by Auth to the user.changes topic. */
public record UserChange(UserChangeType userChangeType, UUID userId) {
    public enum UserChangeType {
        CREATE,
        DELETE
    }
}
