CREATE TABLE IF NOT EXISTS customers (
                                         id UUID PRIMARY KEY,
                                         name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    city_town VARCHAR(120),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP

);
