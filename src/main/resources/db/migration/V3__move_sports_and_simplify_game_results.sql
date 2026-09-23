CREATE TABLE sports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    min_players INT NOT NULL DEFAULT 2,
    max_players INT NOT NULL DEFAULT 32,
    icon_url VARCHAR(255)
);

ALTER TABLE game_results
    DROP COLUMN reported_by,
    DROP COLUMN played_at;

ALTER TABLE game_results
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE game_results
    ADD CONSTRAINT fk_game_results_sport
        FOREIGN KEY (sport_id) REFERENCES sports(id);

ALTER TABLE player_sport_stats
    ADD CONSTRAINT fk_player_sport_stats_sport
        FOREIGN KEY (sport_id) REFERENCES sports(id);
