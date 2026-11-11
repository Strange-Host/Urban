CREATE TABLE IF NOT EXISTS couriers (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    vehicle_info VARCHAR(255),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_couriers_phone_number ON couriers (phone_number);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_id UUID REFERENCES couriers(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_assigned_at TIMESTAMP;