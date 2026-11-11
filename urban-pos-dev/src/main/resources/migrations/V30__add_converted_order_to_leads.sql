ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS converted_order_id UUID REFERENCES orders(id);