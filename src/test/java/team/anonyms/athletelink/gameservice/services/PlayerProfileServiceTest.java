package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.PlayerSportDetailsDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSummaryDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerTotalsProjection;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerProfileServiceTest {

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private PlayerSportStatsRepository playerSportStatsRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private PlayerTotalsProjection playerTotalsProjection;

    @Test
    void summaryContainsAggregatedStatistics() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        PlayerProfile profile = PlayerProfile.builder()
                .id(profileId)
                .userId(userId)
                .build();

        when(playerProfileRepository.findByUserId(userId))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository.findTotalsByProfileId(profileId))
                .thenReturn(playerTotalsProjection);
        when(playerTotalsProjection.getWins()).thenReturn(7L);
        when(playerTotalsProjection.getLosses()).thenReturn(4L);
        when(playerTotalsProjection.getDraws()).thenReturn(1L);
        when(playerTotalsProjection.getSportsPlayed()).thenReturn(2L);

        PlayerSummaryDto result = new PlayerProfileService(
                playerProfileRepository,
                playerSportStatsRepository,
                sportRepository
        ).getSummary(userId);

        assertEquals(userId, result.userId());
        assertEquals(2L, result.summary().sportsPlayed());
        assertEquals(12, result.summary().gamesPlayed());
        assertEquals(7, result.summary().wins());
        assertEquals(4, result.summary().losses());
        assertEquals(1, result.summary().draws());
    }

    @Test
    void sportDetailsContainsStatisticsPlaceAndRank() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        PlayerProfile profile = PlayerProfile.builder()
                .id(profileId)
                .userId(userId)
                .build();

        Sport football = Sport.builder()
                .id(1L)
                .name("Football")
                .build();

        PlayerSportStats stats = PlayerSportStats.builder()
                .id(UUID.randomUUID())
                .playerProfile(profile)
                .sport(football)
                .wins(5)
                .losses(2)
                .draws(1)
                .score(1200)
                .build();

        when(playerProfileRepository.findByUserId(userId))
                .thenReturn(Optional.of(profile));
        when(sportRepository.findById(1L))
                .thenReturn(Optional.of(football));
        when(playerSportStatsRepository
                .findByPlayerProfileIdAndSportId(profileId, 1L))
                .thenReturn(Optional.of(stats));
        when(playerSportStatsRepository.countPlayersAhead(1L, 1200))
                .thenReturn(6L);

        PlayerSportDetailsDto result = new PlayerProfileService(
                playerProfileRepository,
                playerSportStatsRepository,
                sportRepository
        ).getSportDetails(userId, 1L);

        assertEquals(userId, result.userId());
        assertEquals(1L, result.sportId());
        assertEquals("Football", result.sportName());
        assertEquals(8, result.gamesPlayed());
        assertEquals(5, result.wins());
        assertEquals(2, result.losses());
        assertEquals(1, result.draws());
        assertEquals(1200, result.score());
        assertEquals(7L, result.leaderboardPlace());
        assertEquals("GOLD", result.rank().name());
        assertEquals(1, result.rank().tier());
        assertEquals(1200, result.rank().scoreMin());
        assertEquals(1399, result.rank().scoreMax());
    }
}
