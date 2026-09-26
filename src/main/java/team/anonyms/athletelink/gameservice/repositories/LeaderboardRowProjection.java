package team.anonyms.athletelink.gameservice.repositories;

import java.util.UUID;

public interface LeaderboardRowProjection {

    UUID getPlayerProfileId();

    UUID getUserId();

    Integer getScore();

    Integer getWins();

    Integer getLosses();

    Integer getDraws();

    Long getPlace();
}
