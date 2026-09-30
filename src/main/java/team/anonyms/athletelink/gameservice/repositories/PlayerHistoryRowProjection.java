package team.anonyms.athletelink.gameservice.repositories;

import java.time.Instant;
import java.util.UUID;

public interface PlayerHistoryRowProjection {

    UUID getRoundResultId();

    UUID getActivityRequestId();

    Integer getRoundNumber();

    Long getSportId();

    String getOutcome();

    Integer getRatingChange();

    Instant getCreatedAt();
}
