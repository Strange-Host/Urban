-- One row per physical unit that has a printed barcode sticker
CREATE SEQUENCE IF NOT EXISTS stock_unit_serial_seq START 1;

CREATE TABLE stock_units (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                             product_id UUID NOT NULL REFERENCES products(id),
                             serial VARCHAR(32) NOT NULL UNIQUE,
                             batch_id UUID NOT NULL,
                             status VARCHAR(30) NOT NULL,

                             order_id UUID REFERENCES orders(id),
                             scanned_at TIMESTAMPTZ,
                             scanned_by UUID,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMPTZ,
                             deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_stock_units_batch ON stock_units(batch_id);
CREATE INDEX idx_stock_units_order_product ON stock_units(order_id, product_id);
