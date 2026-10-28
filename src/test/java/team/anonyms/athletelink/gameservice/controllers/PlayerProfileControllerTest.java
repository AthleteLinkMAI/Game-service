package team.anonyms.athletelink.gameservice.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.anonyms.athletelink.gameservice.dto.BestSportDto;
import team.anonyms.athletelink.gameservice.dto.LastGameDto;
import team.anonyms.athletelink.gameservice.dto.PlayerRoundHistoryDto;
import team.anonyms.athletelink.gameservice.dto.RankDto;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.services.PlayerProfileService;
import team.anonyms.athletelink.gameservice.services.RoundResultService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlayerProfileControllerTest {

    @Mock PlayerProfileService playerProfileService;
    @Mock RoundResultService roundResultService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new PlayerProfileController(playerProfileService, roundResultService)).build();
    }

    @Test
    void bestSportRouteReturnsSportSpecificStatistics() throws Exception {
        UUID userId = UUID.randomUUID();
        when(playerProfileService.getBestSport(userId)).thenReturn(Optional.of(
                new BestSportDto(7L, 2200, 2500, 3L, 5, 62.5,
                        new RankDto("GOLD", 1, 2000, 2499))));

        mvc.perform(get("/players/{userId}/best-sport", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sportId").value(7))
                .andExpect(jsonPath("$.nextRankScore").value(2500))
                .andExpect(jsonPath("$.leaderboardPlace").value(3))
                .andExpect(jsonPath("$.winRatePercent").value(62.5));
    }

    @Test
    void missingSportStatisticsReturnEmptyResponses() throws Exception {
        UUID userId = UUID.randomUUID();
        when(playerProfileService.getBestSport(userId)).thenReturn(Optional.empty());
        when(playerProfileService.getOtherSports(userId)).thenReturn(List.of());

        mvc.perform(get("/players/{userId}/best-sport", userId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/players/{userId}/other-sports", userId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void lastGameRouteReturnsOnlyFieldsOwnedByGameService() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(roundResultService.getLastGame(userId)).thenReturn(Optional.of(
                new LastGameDto(UUID.randomUUID(), requestId, 2, 7L, GameOutcome.WIN,
                        Instant.parse("2026-10-20T10:00:00Z"), List.of(userId))));

        mvc.perform(get("/players/{userId}/last-game", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$.roundNumber").value(2))
                .andExpect(jsonPath("$.outcome").value("WIN"))
                .andExpect(jsonPath("$.playerIds[0]").value(userId.toString()))
                .andExpect(jsonPath("$.gameName").doesNotExist())
                .andExpect(jsonPath("$.sportName").doesNotExist());
    }

    @Test
    void noRoundsReturnNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        when(roundResultService.getLastGame(userId)).thenReturn(Optional.empty());

        mvc.perform(get("/players/{userId}/last-game", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void matchesUsePageAndCountPerPagePathVariables() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(roundResultService.getPlayerMatches(userId, 7L, 3, 5))
                .thenReturn(List.of(new PlayerRoundHistoryDto(
                        UUID.randomUUID(), requestId, 2, 7L, GameOutcome.WIN, 25,
                        Instant.parse("2026-10-20T10:00:00Z"))));

        mvc.perform(get("/players/{userId}/matches/{page}/{cntPerPage}", userId, 3, 5)
                        .param("sportId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$[0].roundNumber").value(2))
                .andExpect(jsonPath("$[0].outcome").value("WIN"));
    }

    @Test
    void emptyMatchesPageReturnsEmptyArray() throws Exception {
        UUID userId = UUID.randomUUID();
        when(roundResultService.getPlayerMatches(userId, null, 3, 5))
                .thenReturn(List.of());

        mvc.perform(get("/players/{userId}/matches/{page}/{cntPerPage}", userId, 3, 5))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void oldMatchesRouteIsNotAvailable() throws Exception {
        mvc.perform(get("/players/{userId}/matches", UUID.randomUUID())
                        .param("page", "3")
                        .param("cntPerPage", "5"))
                .andExpect(status().isNotFound());
    }
}
