-- PostgreSQL Authoritative Battle Royale Database Schema

CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(32) UNIQUE NOT NULL,
    account_level INT DEFAULT 1,
    current_xp INT DEFAULT 0,
    bp_coins BIGINT DEFAULT 10000,
    vortex_credits INT DEFAULT 500,
    rank_tier VARCHAR(24) DEFAULT 'Bronze I',
    rank_points INT DEFAULT 1000,
    is_banned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS match_records (
    match_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id VARCHAR(64) NOT NULL,
    game_mode VARCHAR(32) NOT NULL,
    map_name VARCHAR(64) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    winning_player_id VARCHAR(64) NOT NULL,
    total_participants INT NOT NULL DEFAULT 100
);

CREATE TABLE IF NOT EXISTS player_match_stats (
    id BIGSERIAL PRIMARY KEY,
    match_id UUID REFERENCES match_records(match_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    placement INT NOT NULL,
    kills INT NOT NULL DEFAULT 0,
    damage_dealt NUMERIC(8,2) NOT NULL DEFAULT 0.0,
    headshots INT NOT NULL DEFAULT 0,
    survival_seconds INT NOT NULL DEFAULT 0,
    xp_reward INT NOT NULL DEFAULT 0,
    bp_reward INT NOT NULL DEFAULT 0,
    rating_delta INT NOT NULL DEFAULT 0
);

-- Transaction function to record server-authoritative match results
CREATE OR REPLACE FUNCTION record_match_settlement(
    p_user_id UUID,
    p_placement INT,
    p_kills INT,
    p_damage NUMERIC,
    p_survival_time INT
) RETURNS VOID AS $$
DECLARE
    v_xp INT := (p_survival_time / 2) + (p_kills * 50) + (CASE WHEN p_placement = 1 THEN 500 ELSE 100 END);
    v_bp INT := (p_kills * 40) + (p_damage * 0.25)::INT + 150;
    v_rp INT := (CASE WHEN p_placement = 1 THEN 35 WHEN p_placement <= 10 THEN 15 ELSE -10 END) + (p_kills * 3);
BEGIN
    UPDATE users 
    SET current_xp = current_xp + v_xp,
        bp_coins = bp_coins + v_bp,
        rank_points = GREATEST(1000, rank_points + v_rp)
    WHERE user_id = p_user_id AND is_banned = FALSE;
END;
$$ LANGUAGE plpgsql;
