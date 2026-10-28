package team.anonyms.athletelink.gameservice.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.anonyms.athletelink.gameservice.dto.LeaderboardEntryDto;
import team.anonyms.athletelink.gameservice.services.LeaderboardService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

    @Mock LeaderboardService leaderboardService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LeaderboardController(leaderboardService)).build();
    }

    @Test
    void numberedPageReturnsArrayWithRequestedFields() throws Exception {
        UUID userId = UUID.randomUUID();
        when(leaderboardService.getLeaderboard(7L, 2, 10)).thenReturn(List.of(
                new LeaderboardEntryDto(11, userId, 2200, 5, 2, 1,
                        new LeaderboardEntryDto.Rank("GOLD", 2000, 2499))));

        mvc.perform(get("/leaderboards/{sportId}/{page}/{cntPerPage}", 7, 2, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].place").value(11))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].rank.name").value("GOLD"))
                .andExpect(jsonPath("$[0].rank.scoreMin").value(2000))
                .andExpect(jsonPath("$[0].rank.tier").doesNotExist());
        verify(leaderboardService).getLeaderboard(7L, 2, 10);
    }

    @Test
    void oldLeaderboardRouteIsNotAvailable() throws Exception {
        mvc.perform(get("/leaderboards/{sportId}", 7))
                .andExpect(status().isNotFound());
    }
}
