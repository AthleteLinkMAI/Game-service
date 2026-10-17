package team.anonyms.athletelink.gameservice.dto;


public record PlayerTotalsRow(
        Long wins,
        Long losses,
        Long draws,
        Long sportsPlayed
) {
}
