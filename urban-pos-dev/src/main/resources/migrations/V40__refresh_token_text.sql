-- Refresh tokens are JWTs; 255 characters is not guaranteed to be enough once more claims are added.
ALTER TABLE users ALTER COLUMN refresh_token TYPE TEXT;
