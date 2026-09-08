CREATE TYPE game_result_status AS ENUM ('PENDING', 'CONFIRMED', 'DISPUTED');
CREATE TYPE game_outcome AS ENUM ('WIN', 'LOSS', 'DRAW');

CREATE TABLE player_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    total_points INT NOT NULL DEFAULT 0,
    rating INT NOT NULL DEFAULT 1000,
    games_played INT NOT NULL DEFAULT 0,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    draws INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE game_results (
    id UUID PRIMARY KEY,
    activity_request_id UUID NOT NULL,
    sport_id BIGINT NOT NULL,
    status game_result_status NOT NULL DEFAULT 'PENDING',
    reported_by UUID NOT NULL,
    played_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE player_game_results (
    id UUID PRIMARY KEY,
    game_result_id UUID NOT NULL REFERENCES game_results(id) ON DELETE CASCADE,
    player_profile_id UUID NOT NULL REFERENCES player_profiles(id),
    score INT NOT NULL DEFAULT 0,
    outcome game_outcome NOT NULL,
    rating_change INT NOT NULL DEFAULT 0,
    points_earned INT NOT NULL DEFAULT 0,

    UNIQUE (game_result_id, player_profile_id)
);

CREATE TABLE achievements (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon_url VARCHAR(255)
);

CREATE TABLE player_achievements (
    id UUID PRIMARY KEY,
    player_profile_id UUID NOT NULL REFERENCES player_profiles(id),
    achievement_id BIGINT NOT NULL REFERENCES achievements(id),
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (player_profile_id, achievement_id)
);

CREATE INDEX idx_player_profiles_user_id ON player_profiles(user_id);
CREATE INDEX idx_player_profiles_rating ON player_profiles(rating DESC);
CREATE INDEX idx_game_results_activity_request ON game_results(activity_request_id);
CREATE INDEX idx_player_game_results_profile ON player_game_results(player_profile_id);

INSERT INTO achievements (code, name, description) VALUES
('FIRST_WIN', 'Первая победа', 'Выиграйте свой первый матч'),
('TEN_GAMES', 'Десятка', 'Сыграйте 10 матчей'),
('FIFTY_GAMES', 'Полтинник', 'Сыграйте 50 матчей'),
('HUNDRED_GAMES', 'Сотня', 'Сыграйте 100 матчей'),
('FIVE_WINS_STREAK', 'Серия побед', 'Выиграйте 5 матчей подряд'),
('MASTER', 'Мастер спорта', 'Достигните рейтинга 1500');
