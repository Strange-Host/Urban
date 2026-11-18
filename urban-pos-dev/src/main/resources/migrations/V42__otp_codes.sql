-- One-time login codes used to be held in server memory, so a restart (or a second server)
-- lost them. They now live in the database; only a hash of each code is stored.
CREATE TABLE IF NOT EXISTS otp_codes (
    phone_number VARCHAR(64) PRIMARY KEY,
    code_hash    VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMP   NOT NULL,
    sent_at      TIMESTAMP   NOT NULL,
    attempts     INTEGER     NOT NULL DEFAULT 0
);
