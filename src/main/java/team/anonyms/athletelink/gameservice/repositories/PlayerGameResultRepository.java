package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.anonyms.athletelink.gameservice.entities.PlayerGameResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlayerGameResultRepository extends JpaRepository<PlayerGameResult, UUID> {

    List<PlayerGameResult> findAllByGameResultId(UUID gameResultId);

    @Query(value = """
            SELECT
                gr.id AS "gameResultId",
                gr.sport_id AS "sportId",
                pgr.outcome::text AS outcome,
                pgr.rating_change AS "ratingChange",
                gr.created_at AS "createdAt"
            FROM player_game_results pgr
            JOIN game_results gr
                ON gr.id = pgr.game_result_id
            WHERE pgr.player_profile_id = :profileId
              AND (
                  CAST(:sportId AS bigint) IS NULL
                  OR gr.sport_id = CAST(:sportId AS bigint)
              )
              AND (
                  CAST(:cursorCreatedAt AS timestamptz) IS NULL
                  OR gr.created_at < CAST(:cursorCreatedAt AS timestamptz)
                  OR (
                      gr.created_at = CAST(:cursorCreatedAt AS timestamptz)
                      AND gr.id < CAST(:cursorGameResultId AS uuid)
                  )
              )
            ORDER BY gr.created_at DESC, gr.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<PlayerHistoryRowProjection> findHistory(
            @Param("profileId") UUID profileId,
            @Param("sportId") Long sportId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorGameResultId") UUID cursorGameResultId,
            @Param("limit") int limit
    );
}
