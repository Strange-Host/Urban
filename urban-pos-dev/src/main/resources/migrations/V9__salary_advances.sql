CREATE TABLE salary_advances (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                                 requisition_id UUID NOT NULL UNIQUE REFERENCES requisitions(id) ON DELETE CASCADE,
                                 user_id UUID NOT NULL REFERENCES users(id),

                                 amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                                 note TEXT,

                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ,
                                 deleted_at TIMESTAMPTZ
);
