ALTER TABLE game_results
    ADD CONSTRAINT uq_game_results_activity_request UNIQUE (activity_request_id);

ALTER TABLE player_game_results
    DROP COLUMN score,
    DROP COLUMN points_earned;

CREATE TABLE player_sport_stats (
    id UUID PRIMARY KEY,
    player_profile_id UUID NOT NULL REFERENCES player_profiles(id),
    sport_id BIGINT NOT NULL,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    draws INT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_player_sport_stats_profile_sport
        UNIQUE (player_profile_id, sport_id),
    CONSTRAINT chk_player_sport_stats_wins_non_negative
        CHECK (wins >= 0),
    CONSTRAINT chk_player_sport_stats_losses_non_negative
        CHECK (losses >= 0),
    CONSTRAINT chk_player_sport_stats_draws_non_negative
        CHECK (draws >= 0),
    CONSTRAINT chk_player_sport_stats_score_non_negative
        CHECK (score >= 0)
);

INSERT INTO player_sport_stats (id, player_profile_id, sport_id, wins, losses, draws, score)
SELECT
    md5(pgr.player_profile_id::text || ':' || gr.sport_id::text)::uuid,
    pgr.player_profile_id,
    gr.sport_id,
    COUNT(*) FILTER (WHERE pgr.outcome = 'WIN'),
    COUNT(*) FILTER (WHERE pgr.outcome = 'LOSS'),
    COUNT(*) FILTER (WHERE pgr.outcome = 'DRAW'),
    GREATEST(0, COALESCE(SUM(pgr.rating_change), 0))
FROM player_game_results pgr
JOIN game_results gr ON gr.id = pgr.game_result_id
GROUP BY pgr.player_profile_id, gr.sport_id;

DROP INDEX IF EXISTS idx_player_profiles_rating;

ALTER TABLE player_profiles
    DROP COLUMN total_points,
    DROP COLUMN rating,
    DROP COLUMN games_played,
    DROP COLUMN wins,
    DROP COLUMN losses,
    DROP COLUMN draws,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;

DROP TABLE player_achievements;
DROP TABLE achievements;

CREATE INDEX idx_player_sport_stats_profile
    ON player_sport_stats(player_profile_id);

CREATE INDEX idx_player_sport_stats_sport_score
    ON player_sport_stats(sport_id, score DESC);
