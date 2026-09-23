package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.PlayerProfileDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;

import java.util.List;
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

    @Test
    void profileContainsIndependentStatisticsAndCalculatedRankForEverySport() {
        UUID userId = UUID.randomUUID();
        PlayerProfile profile = PlayerProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();
        PlayerSportStats football = PlayerSportStats.builder()
                .playerProfile(profile)
                .sport(Sport.builder().id(1L).name("Football").build())
                .wins(5)
                .losses(2)
                .draws(1)
                .score(1000)
                .build();
        PlayerSportStats tennis = PlayerSportStats.builder()
                .playerProfile(profile)
                .sport(Sport.builder().id(2L).name("Tennis").build())
                .wins(1)
                .losses(3)
                .draws(0)
                .score(1200)
                .build();

        when(playerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(playerSportStatsRepository.findAllByPlayerProfile_IdOrderBySport_Id(profile.getId()))
                .thenReturn(List.of(football, tennis));

        PlayerProfileDto result = new PlayerProfileService(
                playerProfileRepository,
                playerSportStatsRepository
        ).getProfile(userId);

        assertEquals(userId, result.userId());
        assertEquals(2, result.sportStats().size());
        assertEquals("SILVER", result.sportStats().get(0).rank().name());
        assertEquals("GOLD", result.sportStats().get(1).rank().name());
    }
}
