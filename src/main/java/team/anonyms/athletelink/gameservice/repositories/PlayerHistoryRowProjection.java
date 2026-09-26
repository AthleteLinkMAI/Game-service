package team.anonyms.athletelink.gameservice.repositories;

import java.time.Instant;
import java.util.UUID;

public interface PlayerHistoryRowProjection {

    UUID getGameResultId();

    Long getSportId();

    String getOutcome();

    Integer getRatingChange();

    Instant getCreatedAt();
}
