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

CREATE TABLE player_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE
);

CREATE TABLE round_results (
    id UUID PRIMARY KEY,
    activity_request_id UUID NOT NULL,
    round_number INT NOT NULL CHECK (round_number > 0),
    sport_id BIGINT NOT NULL CHECK (sport_id > 0),
    status game_result_status NOT NULL DEFAULT 'PENDING',
    recorded_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_round_results_request_round UNIQUE (activity_request_id, round_number)
);

CREATE TABLE player_round_results (
    id UUID PRIMARY KEY,
    round_result_id UUID NOT NULL REFERENCES round_results(id) ON DELETE CASCADE,
    player_profile_id UUID NOT NULL REFERENCES player_profiles(id),
    outcome game_outcome NOT NULL,
    rating_change INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_player_round_results_round_profile
        UNIQUE (round_result_id, player_profile_id)
);

CREATE TABLE player_sport_stats (
    id UUID PRIMARY KEY,
    player_profile_id UUID NOT NULL REFERENCES player_profiles(id),
    sport_id BIGINT NOT NULL CHECK (sport_id > 0),
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

CREATE INDEX idx_player_round_results_profile
    ON player_round_results(player_profile_id);

CREATE INDEX idx_player_sport_stats_profile
    ON player_sport_stats(player_profile_id);

CREATE INDEX idx_player_sport_stats_sport_score
    ON player_sport_stats(sport_id, score DESC);
