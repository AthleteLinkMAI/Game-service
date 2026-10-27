package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.LeaderboardRow;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.LeaderboardCursor;
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
        LeaderboardRow first = row(2200, 1);
        LeaderboardRow extra = row(2100, 2);
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

    private LeaderboardRow row(int score, long seed) {
        return new LeaderboardRow(
                new UUID(0L, seed), new UUID(1L, seed), score,
                1, 0, 0, seed
        );
    }
}
