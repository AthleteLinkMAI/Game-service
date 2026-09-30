package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerRoundResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerTotalsProjection;

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
    @Mock PlayerTotalsProjection totals;

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
        when(playerSportStatsRepository.findTotalsByProfileId(profile.getId())).thenReturn(totals);
        when(totals.getWins()).thenReturn(7L);
        when(totals.getLosses()).thenReturn(4L);
        when(totals.getDraws()).thenReturn(1L);
        when(totals.getSportsPlayed()).thenReturn(2L);

        var response = service.getSummary(profile.getUserId());
        assertEquals(12, response.summary().roundsPlayed());
        assertEquals(2, response.summary().sportsPlayed());
    }

    @Test
    void sportDetailsNeedsNoLocalSportRow() {
        PlayerProfile profile = profile();
        PlayerSportStats stats = PlayerSportStats.builder().id(UUID.randomUUID())
                .playerProfile(profile).sportId(7L)
                .wins(5).losses(2).draws(1).score(1200).build();
        when(playerProfileRepository.findByUserId(profile.getUserId()))
                .thenReturn(Optional.of(profile));
        when(playerSportStatsRepository.findByPlayerProfile_IdAndSportId(profile.getId(), 7L))
                .thenReturn(Optional.of(stats));
        when(playerSportStatsRepository.countPlayersAhead(7L, 1200)).thenReturn(6L);

        var response = service.getSportDetails(profile.getUserId(), 7L);
        assertEquals(7L, response.sportId());
        assertEquals(8, response.roundsPlayed());
        assertEquals(7, response.leaderboardPlace());
        assertEquals("GOLD", response.rank().name());
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
}
