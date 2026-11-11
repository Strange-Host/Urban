CREATE TABLE payments (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                          order_id UUID REFERENCES orders(id) ON DELETE SET NULL,

                          method VARCHAR(50) NOT NULL,

                          amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                          currency VARCHAR(3) NOT NULL DEFAULT 'KES',

                          transaction_ref VARCHAR(100),
                          payer_phone VARCHAR(30),
                          payer_name VARCHAR(140),

                          payment_date TIMESTAMPTZ NOT NULL,

                          status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

                          recorded_by UUID,
                          reconciled_by UUID,
                          reconciled_at TIMESTAMPTZ,

                          notes TEXT,

                          is_matched BOOLEAN DEFAULT FALSE,
                          is_suspicious BOOLEAN DEFAULT FALSE,

                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ,
                          deleted_at TIMESTAMPTZ
);
