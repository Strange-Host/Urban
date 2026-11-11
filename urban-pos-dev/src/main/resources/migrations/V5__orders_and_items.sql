CREATE TABLE orders (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                        customer_id UUID NOT NULL,
                        assigned_to UUID,

                        order_type VARCHAR(20) NOT NULL,
                        order_status VARCHAR(50) NOT NULL,

                        total_amount NUMERIC(14,2) NOT NULL,
                        paid_amount NUMERIC(14,2) DEFAULT 0,

    -- Since your entity has balance insertable=false/updatable=false
                        balance NUMERIC(14,2) GENERATED ALWAYS AS (total_amount - paid_amount) STORED,

                        delivery_city VARCHAR(100),
                        delivery_notes TEXT,
                        invoice_pdf_url TEXT,

                        created_by UUID,

                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ,
                        deleted_at TIMESTAMPTZ
);

CREATE TABLE order_items (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                             order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id UUID NOT NULL REFERENCES products(id),

                             quantity NUMERIC(14,2) NOT NULL CHECK (quantity > 0),

                             selling_price NUMERIC(14,2) NOT NULL,
                             cost_price NUMERIC(14,2) NOT NULL,
                             line_total NUMERIC(14,2) NOT NULL,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMPTZ,
                             deleted_at TIMESTAMPTZ
);
