package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.LeaderboardEntryDto;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.LeaderboardCursor;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;
import team.anonyms.athletelink.gameservice.repositories.LeaderboardRowProjection;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private PlayerSportStatsRepository playerSportStatsRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private CursorService cursorService;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(
                playerSportStatsRepository,
                sportRepository,
                cursorService
        );
    }

    @Test
    void leaderboardRequestsOneExtraRowAndMapsRank() {
        Long sportId = 1L;
        UUID firstProfileId = UUID.randomUUID();
        UUID secondProfileId = UUID.randomUUID();
        UUID extraProfileId = UUID.randomUUID();

        when(sportRepository.findById(sportId))
                .thenReturn(Optional.of(Sport.builder().id(sportId).name("Football").build()));

        LeaderboardRowProjection first = row(firstProfileId, 1L, 1200, 5, 2, 1, 1L);
        LeaderboardRowProjection second = row(secondProfileId, 2L, 1100, 4, 3, 0, 2L);
        LeaderboardRowProjection extra = row(extraProfileId, 3L, 1000, 3, 4, 0, 3L);

        when(playerSportStatsRepository.findLeaderboard(sportId, null, null, 3))
                .thenReturn(List.of(first, second, extra));
        when(cursorService.encode(any())).thenReturn("next-leaderboard-cursor");

        CursorPageDto<LeaderboardEntryDto> page = service.getLeaderboard(
                sportId,
                null,
                2
        );

        assertEquals(2, page.items().size());
        assertEquals(true, page.hasNext());
        assertEquals("next-leaderboard-cursor", page.nextCursor());
        assertEquals(1200, page.items().get(0).score());
        assertEquals("GOLD", page.items().get(0).rank().name());
        assertEquals(4, page.items().get(1).wins());

        verify(playerSportStatsRepository)
                .findLeaderboard(eq(sportId), eq(null), eq(null), eq(3));
    }

    @Test
    void cursorForAnotherSportIsRejected() {
        Long requestedSportId = 1L;
        when(sportRepository.findById(requestedSportId))
                .thenReturn(Optional.of(Sport.builder().id(requestedSportId).name("Football").build()));
        when(cursorService.decode("cursor", LeaderboardCursor.class))
                .thenReturn(new LeaderboardCursor(
                        "leaderboard",
                        2L,
                        1200,
                        UUID.randomUUID()
                ));

        assertThrows(
                InvalidCursorException.class,
                () -> service.getLeaderboard(requestedSportId, "cursor", 20)
        );

        verifyNoInteractions(playerSportStatsRepository);
    }

    private LeaderboardRowProjection row(
            UUID profileId,
            long userIdSeed,
            int score,
            int wins,
            int losses,
            int draws,
            long place
    ) {
        return new LeaderboardRowProjection() {
            @Override
            public UUID getPlayerProfileId() {
                return profileId;
            }

            @Override
            public UUID getUserId() {
                return new UUID(0, userIdSeed);
            }

            @Override
            public Integer getScore() {
                return score;
            }

            @Override
            public Integer getWins() {
                return wins;
            }

            @Override
            public Integer getLosses() {
                return losses;
            }

            @Override
            public Integer getDraws() {
                return draws;
            }

            @Override
            public Long getPlace() {
                return place;
            }
        };
    }
}
