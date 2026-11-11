CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,

                       email VARCHAR(255) UNIQUE NOT NULL,
                       phone_number VARCHAR(30) NOT NULL,

                       refresh_token VARCHAR(255),
                       refresh_token_expiry TIMESTAMP,

                       role VARCHAR(50) NOT NULL,
                       password VARCHAR(255) NOT NULL,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ,
                       deleted_at TIMESTAMPTZ
);
