CREATE TABLE requisitions (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                              requisition_type VARCHAR(50) NOT NULL,
                              status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

                              title VARCHAR(150) NOT NULL,
                              description TEXT,

                              amount_requested NUMERIC(14,2) NOT NULL CHECK (amount_requested > 0),

                              requested_by UUID NOT NULL REFERENCES users(id),

                              approved_by UUID REFERENCES users(id),
                              approved_at TIMESTAMPTZ,

                              rejected_by UUID REFERENCES users(id),
                              rejected_at TIMESTAMPTZ,
                              rejection_reason TEXT,

                              paid_by UUID REFERENCES users(id),
                              paid_at TIMESTAMPTZ,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMPTZ,
                              deleted_at TIMESTAMPTZ
);
