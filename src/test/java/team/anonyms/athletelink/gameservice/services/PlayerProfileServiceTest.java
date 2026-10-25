package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.PlayerTotalsRow;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerRoundResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerProfileServiceTest {

    @Mock PlayerProfileRepository playerProfileRepository;
    @Mock PlayerSportStatsRepository playerSportStatsRepository;
    @Mock PlayerRoundResultRepository playerRoundResultRepository;

    private PlayerProfileService service;

    @BeforeEach
    void setUp() {
        service = new PlayerProfileService(
                playerProfileRepository, playerSportStatsRepository,
                playerRoundResultRepository);
    }

    @Test
    void summaryCountsRoundsAcrossSports() {
        PlayerProfile profile = profile();
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository.findTotalsByProfileId(profile.getId()))
                .thenReturn(new PlayerTotalsRow(7L, 4L, 1L, 2L));

        var response = service.getSummary(profile.getUserId());
        assertEquals(12, response.summary().roundsPlayed());
        assertEquals(2, response.summary().sportsPlayed());
    }

    @Test
    void sportDetailsNeedsNoLocalSportRow() {
        PlayerProfile profile = profile();
        PlayerSportStats stats = PlayerSportStats.builder().id(UUID.randomUUID())
                .playerProfile(profile).sportId(7L)
                .wins(5).losses(2).draws(1).score(2200).build();
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository.findByPlayerProfile_IdAndSportId(profile.getId(), 7L))
                .thenReturn(Optional.of(stats));
        when(playerSportStatsRepository.countPlayersAhead(7L, 2200)).thenReturn(6L);

        var response = service.getSportDetails(profile.getUserId(), 7L);
        assertEquals(7L, response.sportId());
        assertEquals(8, response.roundsPlayed());
        assertEquals(7, response.leaderboardPlace());
        assertEquals("GOLD", response.rank().name());
    }

    @Test
    void bestSportUsesFirstOrderedSportAndItsOwnWinsAndWinRate() {
        PlayerProfile profile = profile();
        PlayerSportStats best = sportStats(profile, 7L, 2200, 3, 1, 1);
        PlayerSportStats other = sportStats(profile, 8L, 1900, 20, 0, 0);
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository
                .findAllByPlayerProfile_IdOrderByScoreDescWinsDescSportIdAsc(profile.getId()))
                .thenReturn(List.of(best, other));
        when(playerSportStatsRepository.countPlayersAhead(7L, 2200)).thenReturn(6L);

        var response = service.getBestSport(profile.getUserId()).orElseThrow();
        assertEquals(7L, response.sportId());
        assertEquals(2200, response.score());
        assertEquals(2500, response.nextRankScore());
        assertEquals(7, response.leaderboardPlace());
        assertEquals(3, response.wins());
        assertEquals(60.0, response.winRatePercent());
        assertEquals("GOLD", response.rank().name());
        verify(playerSportStatsRepository).countPlayersAhead(7L, 2200);
        verify(playerSportStatsRepository, never()).countPlayersAhead(eq(8L), anyInt());
    }

    @Test
    void otherSportsExcludeBestAndKeepRepositoryOrder() {
        PlayerProfile profile = profile();
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository
                .findAllByPlayerProfile_IdOrderByScoreDescWinsDescSportIdAsc(profile.getId()))
                .thenReturn(List.of(
                        sportStats(profile, 7L, 2200, 3, 1, 1),
                        sportStats(profile, 8L, 1900, 2, 1, 0),
                        sportStats(profile, 9L, 1200, 0, 1, 0)));

        var response = service.getOtherSports(profile.getUserId());
        assertEquals(List.of(8L, 9L), response.stream().map(sport -> sport.sportId()).toList());
        assertEquals(2000, response.getFirst().nextRankScore());
        assertEquals("BRONZE", response.getLast().rank().name());
        assertEquals(1500, response.getLast().nextRankScore());
    }

    @Test
    void emptySportStatisticsHaveNoBestSportAndNoOtherSports() {
        PlayerProfile profile = profile();
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository
                .findAllByPlayerProfile_IdOrderByScoreDescWinsDescSportIdAsc(profile.getId()))
                .thenReturn(List.of());

        assertTrue(service.getBestSport(profile.getUserId()).isEmpty());
        assertTrue(service.getOtherSports(profile.getUserId()).isEmpty());
        verify(playerSportStatsRepository, never()).countPlayersAhead(anyLong(), anyInt());
    }

    @Test
    void unknownProfileIsNotTreatedAsAnEmptySportList() {
        UUID userId = UUID.randomUUID();
        when(playerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBestSport(userId));
        assertThrows(ResourceNotFoundException.class, () -> service.getOtherSports(userId));
    }

    @Test
    void bestSportWithNoRoundsHasZeroWinRate() {
        PlayerProfile profile = profile();
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository
                .findAllByPlayerProfile_IdOrderByScoreDescWinsDescSportIdAsc(profile.getId()))
                .thenReturn(List.of(sportStats(profile, 7L, 1000, 0, 0, 0)));

        assertEquals(0.0, service.getBestSport(profile.getUserId()).orElseThrow().winRatePercent());
    }

    @Test
    void authCreateIsIdempotent() {
        PlayerProfile existing = profile();
        when(playerProfileRepository.findByUserId(existing.getUserId()))
                .thenReturn(Optional.empty(), Optional.of(existing));

        service.createProfile(existing.getUserId());
        service.createProfile(existing.getUserId());
        verify(playerProfileRepository, times(1)).save(any(PlayerProfile.class));
    }

    @Test
    void authDeleteRemovesUserDataButNotSharedRounds() {
        PlayerProfile profile = profile();
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));

        service.deleteProfile(profile.getUserId());
        verify(playerRoundResultRepository).deleteByProfileId(profile.getId());
        verify(playerSportStatsRepository).deleteByProfileId(profile.getId());
        verify(playerProfileRepository).delete(profile);
    }

    @Test
    void authDeleteCanBeReplayed() {
        UUID userId = UUID.randomUUID();
        service.deleteProfile(userId);
        verify(playerProfileRepository).findByUserId(userId);
        verifyNoInteractions(playerRoundResultRepository, playerSportStatsRepository);
    }

    private PlayerProfile profile() {
        return PlayerProfile.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).build();
    }

    private PlayerSportStats sportStats(
            PlayerProfile profile, Long sportId, int score, int wins, int losses, int draws) {
        return PlayerSportStats.builder().id(UUID.randomUUID()).playerProfile(profile)
                .sportId(sportId).score(score).wins(wins).losses(losses).draws(draws).build();
    }
}
