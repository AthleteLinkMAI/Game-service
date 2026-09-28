package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.SubmitGameResultDto;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.PlayerGameHistoryDto;
import team.anonyms.athletelink.gameservice.entities.GameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerGameResult;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.dto.GameResultDetailsDto;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.MatchHistoryCursor;
import team.anonyms.athletelink.gameservice.repositories.GameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerGameResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerHistoryRowProjection;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.time.Instant;
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

    @Mock
    private CursorService cursorService;

    private GameResultService service;

    @BeforeEach
    void setUp() {
        service = new GameResultService(
                gameResultRepository,
                playerGameResultRepository,
                playerProfileService,
                sportRepository,
                cursorService
        );
    }

    @Test
    void submitUpdatesWinLossAndDrawForSelectedSport() {
        UUID activityRequestId = UUID.randomUUID();
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

        assertEquals(resultId, service.submitGameResult(request));
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
            () -> service.submitGameResult(request)
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
                ResourceNotFoundException.class,
            () -> service.submitGameResult(request)
        );

        verify(gameResultRepository, never()).save(any(GameResult.class));
        verifyNoInteractions(playerGameResultRepository, playerProfileService);
    }

    @Test
    void playerCountMustFitSportLimits() {
        UUID activityRequestId = UUID.randomUUID();
        Long sportId = 7L;
        Sport sport = Sport.builder()
                .id(sportId)
                .name("Football")
                .minPlayers(2)
                .maxPlayers(11)
                .build();
        SubmitGameResultDto request = new SubmitGameResultDto(
                activityRequestId,
                sportId,
                List.of(new SubmitGameResultDto.PlayerResultEntry(
                        UUID.randomUUID(),
                        GameOutcome.WIN
                ))
        );

        when(gameResultRepository.findByActivityRequestId(activityRequestId))
                .thenReturn(Optional.empty());
        when(sportRepository.findById(sportId)).thenReturn(Optional.of(sport));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.submitGameResult(request)
        );

        verify(gameResultRepository, never()).save(any(GameResult.class));
        verifyNoInteractions(playerGameResultRepository, playerProfileService);
    }

    @Test
    void paginatedHistoryRequestsOneExtraRowAndBuildsNextCursor() {
        UUID profileId = UUID.randomUUID();
        Long sportId = 7L;
        Sport sport = sport(sportId);
        Instant firstCreatedAt = Instant.parse("2026-09-26T12:00:00Z");
        Instant secondCreatedAt = Instant.parse("2026-09-25T12:00:00Z");
        Instant extraCreatedAt = Instant.parse("2026-09-24T12:00:00Z");

        PlayerHistoryRowProjection first = historyRow(
                UUID.randomUUID(), sportId, "WIN", 15, firstCreatedAt);
        PlayerHistoryRowProjection second = historyRow(
                UUID.randomUUID(), sportId, "LOSS", -10, secondCreatedAt);
        PlayerHistoryRowProjection extra = historyRow(
                UUID.randomUUID(), sportId, "DRAW", 0, extraCreatedAt);

        when(sportRepository.findById(sportId)).thenReturn(Optional.of(sport));
        when(playerGameResultRepository.findHistory(
                profileId, sportId, null, null, 3))
                .thenReturn(List.of(first, second, extra));
        when(cursorService.encode(any())).thenReturn("next-history-cursor");

        CursorPageDto<PlayerGameHistoryDto> page = service.getPlayerHistory(
                profileId,
                sportId,
                null,
                2
        );

        assertEquals(2, page.items().size());
        assertEquals("next-history-cursor", page.nextCursor());
        assertEquals(true, page.hasNext());
        assertEquals(first.getGameResultId(), page.items().get(0).gameResultId());
        assertEquals(GameOutcome.LOSS, page.items().get(1).outcome());
        verify(playerGameResultRepository).findHistory(
                profileId, sportId, null, null, 3);
    }

    @Test
    void historyCursorForAnotherPlayerIsRejected() {
        UUID profileId = UUID.randomUUID();
        UUID anotherProfileId = UUID.randomUUID();
        Long sportId = 7L;
        Sport sport = sport(sportId);

        when(sportRepository.findById(sportId)).thenReturn(Optional.of(sport));
        when(cursorService.decode("cursor", MatchHistoryCursor.class))
                .thenReturn(new MatchHistoryCursor(
                        "match-history",
                        anotherProfileId,
                        sportId,
                        Instant.parse("2026-09-26T12:00:00Z"),
                        UUID.randomUUID()
                ));

        assertThrows(
                InvalidCursorException.class,
                () -> service.getPlayerHistory(profileId, sportId, "cursor", 20)
        );

        verifyNoInteractions(playerGameResultRepository);
    }

    @Test
    void resultByActivityRequestContainsAllPlayerResults() {
        UUID activityRequestId = UUID.randomUUID();
        UUID gameResultId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Sport sport = sport(7L);
        GameResult gameResult = GameResult.builder()
                .id(gameResultId)
                .activityRequestId(activityRequestId)
                .sport(sport)
                .status(GameResultStatus.CONFIRMED)
                .createdAt(Instant.parse("2026-09-26T12:00:00Z"))
                .build();
        PlayerProfile profile = profile(userId);
        PlayerGameResult playerResult = PlayerGameResult.builder()
                .gameResult(gameResult)
                .playerProfile(profile)
                .outcome(GameOutcome.WIN)
                .ratingChange(15)
                .build();

        when(gameResultRepository.findByActivityRequestId(activityRequestId))
                .thenReturn(Optional.of(gameResult));
        when(playerGameResultRepository.findAllByGameResultId(gameResultId))
                .thenReturn(List.of(playerResult));

        GameResultDetailsDto result = service.getGameResultByActivityRequestId(activityRequestId);

        assertEquals(gameResultId, result.id());
        assertEquals(activityRequestId, result.activityRequestId());
        assertEquals(1, result.players().size());
        assertEquals(userId, result.players().get(0).userId());
        assertEquals(GameOutcome.WIN, result.players().get(0).outcome());
        assertEquals(15, result.players().get(0).ratingChange());
    }

    private PlayerHistoryRowProjection historyRow(
            UUID gameResultId,
            Long sportId,
            String outcome,
            int ratingChange,
            Instant createdAt
    ) {
        return new PlayerHistoryRowProjection() {
            @Override
            public UUID getGameResultId() {
                return gameResultId;
            }

            @Override
            public Long getSportId() {
                return sportId;
            }

            @Override
            public String getOutcome() {
                return outcome;
            }

            @Override
            public Integer getRatingChange() {
                return ratingChange;
            }

            @Override
            public Instant getCreatedAt() {
                return createdAt;
            }
        };
    }

    private void stubPlayer(PlayerProfile profile, PlayerSportStats stats, Sport sport) {
        when(playerProfileService.createProfile(profile.getUserId())).thenReturn(profile);
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
