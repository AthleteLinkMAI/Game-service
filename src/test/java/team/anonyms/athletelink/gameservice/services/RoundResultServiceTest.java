package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    @Mock KafkaProducerService kafkaProducerService;

    private RoundResultService service;

    @BeforeEach
    void setUp() {
        service = new RoundResultService(
                roundResultRepository, playerRoundResultRepository,
                playerProfileService, kafkaProducerService);
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
    void historyIncludesRequestAndRoundAndFetchesOnlyRequestedPageSize() {
        UUID profileId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        PlayerHistoryRow row = new PlayerHistoryRow(
                UUID.randomUUID(), requestId, 2, 7L, "WIN", 0,
                Instant.parse("2026-09-30T10:00:00Z"));
        when(playerRoundResultRepository.findHistory(profileId, 7L, 1, 1L))
                .thenReturn(List.of(row));

        List<PlayerRoundHistoryDto> page =
                service.getPlayerHistory(profileId, 7L, 2, 1);
        assertEquals(1, page.size());
        assertEquals(requestId, page.getFirst().activityRequestId());
        assertEquals(2, page.getFirst().roundNumber());
        verify(playerRoundResultRepository).findHistory(profileId, 7L, 1, 1L);
    }

    @Test
    void historyPagePastEndIsEmpty() {
        UUID profileId = UUID.randomUUID();
        when(playerRoundResultRepository.findHistory(profileId, null, 20, 20L))
                .thenReturn(List.of());

        List<PlayerRoundHistoryDto> page = service.getPlayerHistory(profileId, null, 2, 20);
        assertTrue(page.isEmpty());
    }

    @Test
    void historyRejectsInvalidPageAndPageSize() {
        UUID profileId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.getPlayerHistory(profileId, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.getPlayerHistory(profileId, null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getPlayerHistory(profileId, null, 1, 101));
        verifyNoInteractions(playerRoundResultRepository);
    }

    @Test
    void lastGameReturnsPersonalOutcomeAndAllRoundParticipants() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-10-20T10:00:00Z");
        PlayerHistoryRow last = new PlayerHistoryRow(
                roundId, requestId, 2, 7L, "DRAW", 0, recordedAt);
        PlayerProfile other = profile();
        when(playerProfileService.getProfileId(userId)).thenReturn(profileId);
        when(playerRoundResultRepository.findHistory(profileId, null, 1, 0L))
                .thenReturn(List.of(last));
        when(playerRoundResultRepository.findAllByRoundResult_Id(roundId))
                .thenReturn(List.of(
                        PlayerRoundResult.builder()
                                .playerProfile(other).build(),
                        PlayerRoundResult.builder()
                                .playerProfile(PlayerProfile.builder().userId(userId).build()).build()));

        var response = service.getLastGame(userId).orElseThrow();
        assertEquals(roundId, response.roundResultId());
        assertEquals(requestId, response.activityRequestId());
        assertEquals(2, response.roundNumber());
        assertEquals(7L, response.sportId());
        assertEquals(GameOutcome.DRAW, response.outcome());
        assertEquals(recordedAt, response.createdAt());
        assertEquals(List.of(userId, other.getUserId()).stream().sorted().toList(),
                response.playerIds());
    }

    @Test
    void lastGameIsEmptyWhenPlayerHasNoRounds() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        when(playerProfileService.getProfileId(userId)).thenReturn(profileId);
        when(playerRoundResultRepository.findHistory(profileId, null, 1, 0L))
                .thenReturn(List.of());

        assertTrue(service.getLastGame(userId).isEmpty());
        verify(playerRoundResultRepository, never()).findAllByRoundResult_Id(any());
    }

    @Test
    void lastGameRequiresAnExistingProfile() {
        UUID userId = UUID.randomUUID();
        when(playerProfileService.getProfileId(userId))
                .thenThrow(new ResourceNotFoundException("Player profile not found"));

        assertThrows(ResourceNotFoundException.class, () -> service.getLastGame(userId));
        verifyNoInteractions(playerRoundResultRepository);
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
