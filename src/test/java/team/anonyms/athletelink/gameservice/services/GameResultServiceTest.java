package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.SubmitGameResultDto;
import team.anonyms.athletelink.gameservice.entities.GameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerGameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.repositories.GameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerGameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameResultServiceTest {

    @Mock
    private GameResultRepository gameResultRepository;

    @Mock
    private PlayerGameResultRepository playerGameResultRepository;

    @Mock
    private PlayerProfileService playerProfileService;

    @Mock
    private SportRepository sportRepository;

    private GameResultService service;

    @BeforeEach
    void setUp() {
        service = new GameResultService(
                gameResultRepository,
                playerGameResultRepository,
                playerProfileService,
                sportRepository
        );
    }

    @Test
    void submitUpdatesWinLossAndDrawForSelectedSport() {
        UUID activityRequestId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        Long sportId = 7L;
        Sport sport = sport(sportId);

        PlayerProfile winner = profile(UUID.randomUUID());
        PlayerProfile loser = profile(UUID.randomUUID());
        PlayerProfile drawingPlayer = profile(UUID.randomUUID());

        PlayerSportStats winnerStats = stats(winner, sport);
        PlayerSportStats loserStats = stats(loser, sport);
        PlayerSportStats drawStats = stats(drawingPlayer, sport);

        when(gameResultRepository.findByActivityRequestId(activityRequestId)).thenReturn(Optional.empty());
        when(sportRepository.findById(sportId)).thenReturn(Optional.of(sport));
        when(gameResultRepository.save(any(GameResult.class))).thenAnswer(invocation -> {
            GameResult result = invocation.getArgument(0);
            result.setId(resultId);
            return result;
        });

        stubPlayer(winner, winnerStats, sport);
        stubPlayer(loser, loserStats, sport);
        stubPlayer(drawingPlayer, drawStats, sport);

        SubmitGameResultDto request = new SubmitGameResultDto(
                activityRequestId,
                sportId,
                List.of(
                        new SubmitGameResultDto.PlayerResultEntry(winner.getUserId(), GameOutcome.WIN),
                        new SubmitGameResultDto.PlayerResultEntry(loser.getUserId(), GameOutcome.LOSS),
                        new SubmitGameResultDto.PlayerResultEntry(drawingPlayer.getUserId(), GameOutcome.DRAW)
                )
        );

        assertEquals(resultId, service.submitGameResult(request, reporterId));
        assertEquals(1, winnerStats.getWins());
        assertEquals(1, loserStats.getLosses());
        assertEquals(1, drawStats.getDraws());
        assertEquals(0, winnerStats.getScore());
        assertEquals(0, loserStats.getScore());
        assertEquals(0, drawStats.getScore());

        ArgumentCaptor<PlayerGameResult> resultCaptor = ArgumentCaptor.forClass(PlayerGameResult.class);
        verify(playerGameResultRepository, times(3)).save(resultCaptor.capture());
        assertEquals(
                List.of(GameOutcome.WIN, GameOutcome.LOSS, GameOutcome.DRAW),
                resultCaptor.getAllValues().stream().map(PlayerGameResult::getOutcome).toList()
        );
        resultCaptor.getAllValues().forEach(result -> assertEquals(0, result.getRatingChange()));
    }

    @Test
    void duplicatePlayerIsRejectedBeforeAnythingIsSaved() {
        UUID userId = UUID.randomUUID();
        SubmitGameResultDto request = new SubmitGameResultDto(
                UUID.randomUUID(),
                1L,
                List.of(
                        new SubmitGameResultDto.PlayerResultEntry(userId, GameOutcome.WIN),
                        new SubmitGameResultDto.PlayerResultEntry(userId, GameOutcome.LOSS)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.submitGameResult(request, UUID.randomUUID())
        );

        verifyNoInteractions(gameResultRepository, playerGameResultRepository, playerProfileService);
    }

    @Test
    void unknownSportIsRejectedBeforeResultIsSaved() {
        UUID activityRequestId = UUID.randomUUID();
        Long sportId = 99L;
        SubmitGameResultDto request = new SubmitGameResultDto(
                activityRequestId,
                sportId,
                List.of(new SubmitGameResultDto.PlayerResultEntry(UUID.randomUUID(), GameOutcome.WIN))
        );

        when(gameResultRepository.findByActivityRequestId(activityRequestId)).thenReturn(Optional.empty());
        when(sportRepository.findById(sportId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.submitGameResult(request, UUID.randomUUID())
        );

        verify(gameResultRepository, never()).save(any(GameResult.class));
        verifyNoInteractions(playerGameResultRepository, playerProfileService);
    }

    private void stubPlayer(PlayerProfile profile, PlayerSportStats stats, Sport sport) {
        when(playerProfileService.getOrCreateProfile(profile.getUserId())).thenReturn(profile);
        when(playerProfileService.getOrCreateSportStats(profile, sport)).thenReturn(stats);
        when(playerProfileService.saveSportStats(stats)).thenReturn(stats);
    }

    private PlayerProfile profile(UUID userId) {
        return PlayerProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();
    }

    private PlayerSportStats stats(PlayerProfile profile, Sport sport) {
        return PlayerSportStats.builder()
                .id(UUID.randomUUID())
                .playerProfile(profile)
                .sport(sport)
                .build();
    }

    private Sport sport(Long id) {
        return Sport.builder()
                .id(id)
                .name("Sport " + id)
                .build();
    }
}
