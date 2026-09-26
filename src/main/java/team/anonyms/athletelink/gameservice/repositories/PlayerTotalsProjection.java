package team.anonyms.athletelink.gameservice.repositories;

public interface PlayerTotalsProjection {

    Long getWins();

    Long getLosses();

    Long getDraws();

    Long getSportsPlayed();
}