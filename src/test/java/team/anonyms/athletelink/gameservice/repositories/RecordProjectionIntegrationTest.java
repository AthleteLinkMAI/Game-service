package team.anonyms.athletelink.gameservice.repositories;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.LeaderboardRow;
import team.anonyms.athletelink.gameservice.dto.PlayerHistoryRow;
import team.anonyms.athletelink.gameservice.dto.PlayerTotalsRow;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerRoundResult;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.RoundResult;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Runs against a disposable PostgreSQL database when GAME_PROJECTION_TEST_URL is set. */
@EnabledIfEnvironmentVariable(named = "GAME_PROJECTION_TEST_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=${GAME_PROJECTION_TEST_URL}",
        "spring.datasource.username=${GAME_PROJECTION_TEST_USER:postgres}",
        "spring.datasource.password=${GAME_PROJECTION_TEST_PASSWORD:}",
        "spring.kafka.listener.auto-startup=false"
})
@Transactional
class RecordProjectionIntegrationTest {

    @Autowired private EntityManager entityManager;
    @Autowired private PlayerProfileRepository playerProfileRepository;
    @Autowired private PlayerSportStatsRepository playerSportStatsRepository;
    @Autowired private PlayerRoundResultRepository playerRoundResultRepository;
    @Autowired private RoundResultRepository roundResultRepository;

    @Test
    void nativeQueriesConstructAllThreeRecords() {
        PlayerProfile player = playerProfileRepository.save(
                PlayerProfile.builder().userId(UUID.randomUUID()).build());
        PlayerProfile opponent = playerProfileRepository.save(
                PlayerProfile.builder().userId(UUID.randomUUID()).build());
        playerSportStatsRepository.save(PlayerSportStats.builder()
                .playerProfile(player).sportId(7L)
                .wins(3).losses(1).draws(1).score(2200).build());
        playerSportStatsRepository.save(PlayerSportStats.builder()
                .playerProfile(player).sportId(8L)
                .wins(1).losses(2).draws(0).score(1800).build());
        playerSportStatsRepository.save(PlayerSportStats.builder()
                .playerProfile(opponent).sportId(7L)
                .wins(1).losses(3).draws(0).score(1800).build());

        UUID requestId = UUID.randomUUID();
        RoundResult round = roundResultRepository.save(RoundResult.builder()
                .activityRequestId(requestId).roundNumber(2).sportId(7L)
                .status(GameResultStatus.CONFIRMED)
                .build());
        playerRoundResultRepository.save(PlayerRoundResult.builder()
                .roundResult(round).playerProfile(player)
                .outcome(GameOutcome.WIN).ratingChange(25).build());
        entityManager.flush();
        entityManager.clear();

        PlayerTotalsRow totals = playerSportStatsRepository.findTotalsByProfileId(player.getId());
        assertEquals(new PlayerTotalsRow(4L, 3L, 1L, 2L), totals);

        List<LeaderboardRow> leaderboard = playerSportStatsRepository.findLeaderboard(
                7L, null, null, 10);
        assertEquals(2, leaderboard.size());
        assertEquals(player.getId(), leaderboard.getFirst().playerProfileId());
        assertEquals(2200, leaderboard.getFirst().score());
        assertEquals(1L, leaderboard.getFirst().place());

        List<PlayerHistoryRow> history = playerRoundResultRepository.findHistory(
                player.getId(), null, null, null, 10);
        assertEquals(1, history.size());
        assertEquals(requestId, history.getFirst().activityRequestId());
        assertEquals(2, history.getFirst().roundNumber());
        assertEquals("WIN", history.getFirst().outcome());
        assertNotNull(history.getFirst().createdAt());
    }
}
