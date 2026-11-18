-- One row per signed-in device. Logging out revokes only that row, so signing out on one
-- device no longer signs the user out everywhere.
CREATE TABLE IF NOT EXISTS auth_sessions (
    id         UUID      PRIMARY KEY,
    user_id    UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_user ON auth_sessions (user_id);
