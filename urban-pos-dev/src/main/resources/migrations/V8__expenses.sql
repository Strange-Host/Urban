CREATE TABLE expenses (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                          expense_type VARCHAR(50) NOT NULL,
                          amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                          description TEXT,

                          payment_method VARCHAR(50) NOT NULL,

                          reference_code VARCHAR(120),

                          created_by UUID NOT NULL REFERENCES users(id),

                          requisition_id UUID UNIQUE REFERENCES requisitions(id) ON DELETE SET NULL,

                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ,
                          deleted_at TIMESTAMPTZ
);
