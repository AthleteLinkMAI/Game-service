package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.LeaderboardCursor;
import team.anonyms.athletelink.gameservice.repositories.LeaderboardRowProjection;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock PlayerSportStatsRepository statsRepository;
    @Mock CursorService cursorService;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(statsRepository, cursorService);
    }

    @Test
    void leaderboardWorksWithoutSportCatalogAndRequestsExtraRow() {
        LeaderboardRowProjection first = row(2200, 1);
        LeaderboardRowProjection extra = mock(LeaderboardRowProjection.class);
        when(statsRepository.findLeaderboard(7L, null, null, 2))
                .thenReturn(List.of(first, extra));
        when(cursorService.encode(any())).thenReturn("next");

        var page = service.getLeaderboard(7L, null, 1);
        assertTrue(page.hasNext());
        assertEquals("next", page.nextCursor());
        assertEquals("GOLD", page.items().getFirst().rank().name());
        verify(statsRepository).findLeaderboard(7L, null, null, 2);
    }

    @Test
    void cursorForAnotherSportIsRejected() {
        when(cursorService.decode("cursor", LeaderboardCursor.class))
                .thenReturn(new LeaderboardCursor("leaderboard", 8L, 1200, UUID.randomUUID()));
        assertThrows(InvalidCursorException.class,
                () -> service.getLeaderboard(7L, "cursor", 20));
        verifyNoInteractions(statsRepository);
    }

    private LeaderboardRowProjection row(int score, long seed) {
        LeaderboardRowProjection row = mock(LeaderboardRowProjection.class);
        when(row.getScore()).thenReturn(score);
        when(row.getPlayerProfileId()).thenReturn(new UUID(0L, seed));
        when(row.getUserId()).thenReturn(new UUID(1L, seed));
        when(row.getWins()).thenReturn(1);
        when(row.getLosses()).thenReturn(0);
        when(row.getDraws()).thenReturn(0);
        when(row.getPlace()).thenReturn(seed);
        return row;
    }
}
