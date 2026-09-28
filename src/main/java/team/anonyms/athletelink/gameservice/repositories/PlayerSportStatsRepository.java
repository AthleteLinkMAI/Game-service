package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSportStatsRepository
        extends JpaRepository<PlayerSportStats, UUID> {

    Optional<PlayerSportStats> findByPlayerProfileIdAndSportId(
            UUID playerProfileId,
            Long sportId
    );

    @Query(value = """
            SELECT
                COALESCE(SUM(wins), 0) AS wins,
                COALESCE(SUM(losses), 0) AS losses,
                COALESCE(SUM(draws), 0) AS draws,
                COUNT(*) AS "sportsPlayed"
            FROM player_sport_stats
            WHERE player_profile_id = :profileId
            """, nativeQuery = true)
    PlayerTotalsProjection findTotalsByProfileId(
            @Param("profileId") UUID profileId
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM player_sport_stats
        WHERE sport_id = :sportId
          AND score > :score
        """, nativeQuery = true)
    long countPlayersAhead(
            @Param("sportId") Long sportId,
            @Param("score") Integer score
    );

    @Query(value = """
            WITH ranked AS (
                SELECT
                    pss.player_profile_id AS player_profile_id,
                    pp.user_id AS user_id,
                    pss.score AS score,
                    pss.wins AS wins,
                    pss.losses AS losses,
                    pss.draws AS draws,
                    RANK() OVER (
                        ORDER BY pss.score DESC
                    ) AS place
                FROM player_sport_stats pss
                JOIN player_profiles pp
                    ON pp.id = pss.player_profile_id
                WHERE pss.sport_id = :sportId
            )
            SELECT
                player_profile_id AS "playerProfileId",
                user_id AS "userId",
                score AS score,
                wins AS wins,
                losses AS losses,
                draws AS draws,
                place AS place
            FROM ranked
            WHERE
                CAST(:cursorScore AS integer) IS NULL
                OR score < CAST(:cursorScore AS integer)
                OR (
                    score = CAST(:cursorScore AS integer)
                    AND player_profile_id > CAST(:cursorProfileId AS uuid)
                )
            ORDER BY score DESC, player_profile_id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<LeaderboardRowProjection> findLeaderboard(
            @Param("sportId") Long sportId,
            @Param("cursorScore") Integer cursorScore,
            @Param("cursorProfileId") UUID cursorProfileId,
            @Param("limit") int limit
    );
}
