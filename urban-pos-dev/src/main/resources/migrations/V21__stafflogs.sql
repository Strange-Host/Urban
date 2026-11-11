CREATE TABLE staff_logs (
                            id UUID PRIMARY KEY,

                            staff_id VARCHAR(100) NOT NULL,

                            action VARCHAR(50) NOT NULL,

                            entity VARCHAR(50) NOT NULL,

                            entity_id VARCHAR(100),

                            endpoint TEXT,

                            method VARCHAR(10),

                            description TEXT,

                            timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
