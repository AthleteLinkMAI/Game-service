package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.anonyms.athletelink.gameservice.entities.PlayerRoundResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlayerRoundResultRepository extends JpaRepository<PlayerRoundResult, UUID> {

    List<PlayerRoundResult> findAllByRoundResult_Id(UUID roundResultId);

    @Modifying
    @Query("DELETE FROM PlayerRoundResult result WHERE result.playerProfile.id = :profileId")
    void deleteByProfileId(@Param("profileId") UUID profileId);

    @Query(value = """
            SELECT
                rr.id AS "roundResultId",
                rr.activity_request_id AS "activityRequestId",
                rr.round_number AS "roundNumber",
                rr.sport_id AS "sportId",
                prr.outcome::text AS outcome,
                prr.rating_change AS "ratingChange",
                rr.created_at AS "createdAt"
            FROM player_round_results prr
            JOIN round_results rr
                ON rr.id = prr.round_result_id
            WHERE prr.player_profile_id = :profileId
              AND (
                  CAST(:sportId AS bigint) IS NULL
                  OR rr.sport_id = CAST(:sportId AS bigint)
              )
              AND (
                  CAST(:cursorCreatedAt AS timestamptz) IS NULL
                  OR rr.created_at < CAST(:cursorCreatedAt AS timestamptz)
                  OR (
                      rr.created_at = CAST(:cursorCreatedAt AS timestamptz)
                      AND rr.id < CAST(:cursorRoundResultId AS uuid)
                  )
              )
            ORDER BY rr.created_at DESC, rr.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<PlayerHistoryRowProjection> findHistory(
            @Param("profileId") UUID profileId,
            @Param("sportId") Long sportId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorRoundResultId") UUID cursorRoundResultId,
            @Param("limit") int limit
    );
}
