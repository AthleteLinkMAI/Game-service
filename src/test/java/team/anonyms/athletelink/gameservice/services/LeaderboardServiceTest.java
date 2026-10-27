package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.LeaderboardRow;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock PlayerSportStatsRepository statsRepository;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(statsRepository);
    }

    @Test
    void leaderboardReadsRequestedPageWithoutSportCatalog() {
        LeaderboardRow second = row(2200, 2);
        when(statsRepository.findLeaderboard(7L, 1, 1L)).thenReturn(List.of(second));

        var page = service.getLeaderboard(7L, 2, 1);
        assertEquals(1, page.size());
        assertEquals(2, page.getFirst().place());
        assertEquals("GOLD", page.getFirst().rank().name());
        assertEquals(2000, page.getFirst().rank().scoreMin());
        verify(statsRepository).findLeaderboard(7L, 1, 1L);
    }

    @Test
    void invalidPageAndPageSizeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getLeaderboard(7L, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.getLeaderboard(7L, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getLeaderboard(7L, 1, 101));
        assertThrows(IllegalArgumentException.class, () -> service.getLeaderboard(0L, 1, 20));
        verifyNoInteractions(statsRepository);
    }

    @Test
    void pagePastEndReturnsEmptyArray() {
        when(statsRepository.findLeaderboard(7L, 20, 40L)).thenReturn(List.of());
        assertTrue(service.getLeaderboard(7L, 3, 20).isEmpty());
    }

    private LeaderboardRow row(int score, long seed) {
        return new LeaderboardRow(
                new UUID(1L, seed), score,
                1, 0, 0, seed
        );
    }
}
