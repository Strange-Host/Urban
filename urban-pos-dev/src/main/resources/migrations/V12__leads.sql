CREATE TABLE leads (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       location VARCHAR(255) NOT NULL,
                       phone_number VARCHAR(50) NOT NULL,
                       alternative_phone_number VARCHAR(50),
                       extra_details TEXT,
                       wp_entry_id BIGINT,
                       created TIMESTAMP NOT NULL,
                       status VARCHAR(50) DEFAULT 'NEW'
);
