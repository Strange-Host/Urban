ALTER TABLE products
    ADD COLUMN IF NOT EXISTS assigned_staff_id UUID REFERENCES users(id);




