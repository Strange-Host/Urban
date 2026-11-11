CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               message TEXT,
                               type VARCHAR(100),
                               reference_id BIGINT,
                               is_read BOOLEAN DEFAULT FALSE,
                               sent_at TIMESTAMP,
                               created_at TIMESTAMP DEFAULT now(),
                               updated_at TIMESTAMP DEFAULT now()
);

