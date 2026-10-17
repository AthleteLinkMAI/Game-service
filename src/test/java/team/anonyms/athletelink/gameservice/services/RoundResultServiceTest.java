package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.PlayerRoundHistoryDto;
import team.anonyms.athletelink.gameservice.dto.RoundResultDetailsDto;
import team.anonyms.athletelink.gameservice.dto.SubmitRoundResultDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerRoundResult;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.RoundResult;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.dto.PlayerHistoryRow;
import team.anonyms.athletelink.gameservice.ranks.RankTiers;
import team.anonyms.athletelink.gameservice.repositories.PlayerRoundResultRepository;
import team.anonyms.athletelink.gameservice.repositories.RoundResultRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundResultServiceTest {
    @Mock RoundResultRepository roundResultRepository;
    @Mock PlayerRoundResultRepository playerRoundResultRepository;
    @Mock PlayerProfileService playerProfileService;
    @Mock CursorService cursorService;
    @Mock KafkaProducerService kafkaProducerService;

    private RoundResultService service;

    @BeforeEach
    void setUp() {
        service = new RoundResultService(
                roundResultRepository, playerRoundResultRepository,
                playerProfileService, cursorService, kafkaProducerService);
    }

    @Test
    void twoRoundsOfOneRequestAreStoredAndCountedSeparately() {
        UUID requestId = UUID.randomUUID();
        PlayerProfile winner = profile();
        PlayerProfile loser = profile();
        PlayerSportStats winnerStats = stats(winner);
        PlayerSportStats loserStats = stats(loser);
        when(playerProfileService.requireProfileForUpdate(winner.getUserId())).thenReturn(winner);
        when(playerProfileService.requireProfileForUpdate(loser.getUserId())).thenReturn(loser);
        when(playerProfileService.getOrCreateSportStats(winner, 7L)).thenReturn(winnerStats);
        when(playerProfileService.getOrCreateSportStats(loser, 7L)).thenReturn(loserStats);
        when(roundResultRepository.save(any(RoundResult.class))).thenAnswer(invocation -> {
            RoundResult round = invocation.getArgument(0);
            round.setId(UUID.randomUUID());
            return round;
        });

        SubmitRoundResultDto first = event(requestId, 1, winner, loser);
        SubmitRoundResultDto second = event(requestId, 2, winner, loser);
        assertNotNull(service.submitRoundResult(first));
        assertNotNull(service.submitRoundResult(second));

        assertEquals(2, winnerStats.getWins());
        assertEquals(2, loserStats.getLosses());
        verify(roundResultRepository).findByActivityRequestIdAndRoundNumber(requestId, 1);
        verify(roundResultRepository).findByActivityRequestIdAndRoundNumber(requestId, 2);
        verify(roundResultRepository, times(2)).save(any(RoundResult.class));
        verify(playerRoundResultRepository, times(4)).save(any(PlayerRoundResult.class));
    }

    @Test
    void sameEventReplayDoesNotAwardPointsTwice() {
        UUID requestId = UUID.randomUUID();
        PlayerProfile winner = profile();
        PlayerProfile loser = profile();
        UUID roundId = UUID.randomUUID();
        RoundResult existing = RoundResult.builder()
                .id(roundId).activityRequestId(requestId).roundNumber(1)
                .sportId(7L).status(GameResultStatus.CONFIRMED).build();
        when(roundResultRepository.findByActivityRequestIdAndRoundNumber(requestId, 1))
                .thenReturn(Optional.of(existing));
        assertEquals(roundId, service.submitRoundResult(event(requestId, 1, winner, loser)));
        verify(roundResultRepository, never()).save(any());
        verifyNoInteractions(playerRoundResultRepository, playerProfileService);
    }

    @Test
    void replayWithDifferentPayloadStillDoesNotReadPlayerResultsOrAwardPoints() {
        UUID requestId = UUID.randomUUID();
        PlayerProfile winner = profile();
        PlayerProfile loser = profile();
        RoundResult existing = RoundResult.builder()
                .id(UUID.randomUUID()).activityRequestId(requestId)
                .roundNumber(1).sportId(7L).build();
        when(roundResultRepository.findByActivityRequestIdAndRoundNumber(requestId, 1))
                .thenReturn(Optional.of(existing));
        SubmitRoundResultDto differentPayload = new SubmitRoundResultDto(
                "ROUND_RESULT", UUID.randomUUID(), requestId, 1, 99L,
                UUID.randomUUID(), List.of(
                        new SubmitRoundResultDto.PlayerResultEntry(winner.getUserId(), GameOutcome.LOSS),
                        new SubmitRoundResultDto.PlayerResultEntry(loser.getUserId(), GameOutcome.WIN)));

        assertEquals(existing.getId(), service.submitRoundResult(differentPayload));
        verify(roundResultRepository, never()).save(any());
        verifyNoInteractions(playerRoundResultRepository, playerProfileService);
    }

    @Test
    void replayAfterAuthDeletedOnePlayerIsStillIdempotent() {
        UUID requestId = UUID.randomUUID();
        PlayerProfile winner = profile();
        PlayerProfile deletedLoser = profile();
        RoundResult existing = RoundResult.builder()
                .id(UUID.randomUUID()).activityRequestId(requestId)
                .roundNumber(1).sportId(7L).build();
        when(roundResultRepository.findByActivityRequestIdAndRoundNumber(requestId, 1))
                .thenReturn(Optional.of(existing));
        assertEquals(existing.getId(),
                service.submitRoundResult(event(requestId, 1, winner, deletedLoser)));
        verify(roundResultRepository, never()).save(any());
        verifyNoInteractions(playerRoundResultRepository, playerProfileService);
    }

    @Test
    void missingAuthProfileStopsRoundWithoutCreatingIt() {
        PlayerProfile winner = profile();
        PlayerProfile loser = profile();
        when(playerProfileService.requireProfileForUpdate(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Player profile not found"));

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitRoundResult(event(UUID.randomUUID(), 1, winner, loser)));
        verify(roundResultRepository, never()).save(any());
        verify(playerRoundResultRepository, never()).save(any());
    }

    @Test
    void byRequestReturnsEveryRoundInRepositoryOrder() {
        UUID requestId = UUID.randomUUID();
        RoundResult first = RoundResult.builder().id(UUID.randomUUID())
                .activityRequestId(requestId).roundNumber(1).sportId(7L)
                .status(GameResultStatus.CONFIRMED).build();
        RoundResult second = RoundResult.builder().id(UUID.randomUUID())
                .activityRequestId(requestId).roundNumber(2).sportId(7L)
                .status(GameResultStatus.CONFIRMED).build();
        when(roundResultRepository.findAllByActivityRequestIdOrderByRoundNumberAsc(requestId))
                .thenReturn(List.of(first, second));

        List<RoundResultDetailsDto> response = service.getRoundResultsByActivityRequestId(requestId);
        assertEquals(List.of(1, 2), response.stream().map(RoundResultDetailsDto::roundNumber).toList());
        verify(playerRoundResultRepository).findAllByRoundResult_Id(first.getId());
        verify(playerRoundResultRepository).findAllByRoundResult_Id(second.getId());
    }

    @Test
    void historyIncludesRequestAndRoundAndFetchesLimitPlusOne() {
        UUID profileId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        PlayerHistoryRow row = new PlayerHistoryRow(
                UUID.randomUUID(), requestId, 2, 7L, "WIN", 0,
                Instant.parse("2026-09-30T10:00:00Z"));
        PlayerHistoryRow extra = new PlayerHistoryRow(
                UUID.randomUUID(), requestId, 1, 7L, "LOSS", 0,
                Instant.parse("2026-09-29T10:00:00Z"));
        when(playerRoundResultRepository.findHistory(profileId, 7L, null, null, 2))
                .thenReturn(List.of(row, extra));
        when(cursorService.encode(any())).thenReturn("next");

        CursorPageDto<PlayerRoundHistoryDto> page =
                service.getPlayerHistory(profileId, 7L, null, 1);
        assertTrue(page.hasNext());
        assertEquals("next", page.nextCursor());
        assertEquals(requestId, page.items().getFirst().activityRequestId());
        assertEquals(2, page.items().getFirst().roundNumber());
        verify(playerRoundResultRepository).findHistory(profileId, 7L, null, null, 2);
    }

    private SubmitRoundResultDto event(
            UUID requestId, int roundNumber, PlayerProfile winner, PlayerProfile loser) {
        return new SubmitRoundResultDto("ROUND_RESULT", UUID.randomUUID(), requestId,
                roundNumber, 7L, UUID.randomUUID(), List.of(
                new SubmitRoundResultDto.PlayerResultEntry(winner.getUserId(), GameOutcome.WIN),
                new SubmitRoundResultDto.PlayerResultEntry(loser.getUserId(), GameOutcome.LOSS)));
    }

    private PlayerProfile profile() {
        return PlayerProfile.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).build();
    }

    private PlayerSportStats stats(PlayerProfile profile) {
        return PlayerSportStats.builder().id(UUID.randomUUID()).playerProfile(profile)
                .sportId(7L).score(RankTiers.INITIAL_SCORE).build();
    }
}
