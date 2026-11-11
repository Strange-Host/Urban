CREATE TABLE logout_checks (
                               id UUID PRIMARY KEY,
                               user_id UUID,
                               orders_done BOOLEAN,
                               orders_reason TEXT,
                               parcels_done BOOLEAN,
                               parcels_reason TEXT,
                               notes TEXT,
                               created_at TIMESTAMP
);
