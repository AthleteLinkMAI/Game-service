CREATE TYPE game_result_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'DISPUTED'
);

CREATE TYPE game_outcome AS ENUM (
    'WIN',
    'LOSS',
    'DRAW'
);

CREATE TABLE sports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    min_players INT NOT NULL DEFAULT 2,
    max_players INT NOT NULL DEFAULT 32,
    icon_url VARCHAR(255)
);

CREATE TABLE player_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE
);

CREATE TABLE game_results (
    id UUID PRIMARY KEY,
    activity_request_id UUID NOT NULL UNIQUE,
    sport_id BIGINT NOT NULL REFERENCES sports(id),
    status game_result_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE player_game_results (
    id UUID PRIMARY KEY,
    game_result_id UUID NOT NULL REFERENCES game_results(id) ON DELETE CASCADE,
    player_profile_id UUID NOT NULL REFERENCES player_profiles(id),
    outcome game_outcome NOT NULL,
    rating_change INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_player_game_results_game_profile
        UNIQUE (game_result_id, player_profile_id)
);

CREATE TABLE player_sport_stats (
    id UUID PRIMARY KEY,
    player_profile_id UUID NOT NULL REFERENCES player_profiles(id),
    sport_id BIGINT NOT NULL REFERENCES sports(id),
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    draws INT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_player_sport_stats_profile_sport UNIQUE (player_profile_id, sport_id),
    CONSTRAINT chk_player_sport_stats_wins_non_negative CHECK (wins >= 0),
    CONSTRAINT chk_player_sport_stats_losses_non_negative CHECK (losses >= 0),
    CONSTRAINT chk_player_sport_stats_draws_non_negative CHECK (draws >= 0),
    CONSTRAINT chk_player_sport_stats_score_non_negative CHECK (score >= 0)
);

CREATE INDEX idx_player_profiles_user_id
    ON player_profiles(user_id);

CREATE INDEX idx_game_results_activity_request
    ON game_results(activity_request_id);

CREATE INDEX idx_player_game_results_profile
    ON player_game_results(player_profile_id);

CREATE INDEX idx_player_sport_stats_profile
    ON player_sport_stats(player_profile_id);

CREATE INDEX idx_player_sport_stats_sport_score
    ON player_sport_stats(sport_id, score DESC);
