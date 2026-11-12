-- pgcrypto backs gen_random_uuid() used by V20/V25/V31 on servers older than PostgreSQL 13.
-- It cannot go into V1 without invalidating the checksum of an applied migration.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- V28 added an unquoted "productOptions", which PostgreSQL folded to "productoptions",
-- while the entity maps "product_options". Reconcile both columns.
ALTER TABLE leads ADD COLUMN IF NOT EXISTS product_options TEXT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'leads' AND column_name = 'productoptions'
    ) THEN
        EXECUTE 'UPDATE leads SET product_options = productoptions
                 WHERE product_options IS NULL AND productoptions IS NOT NULL';
    END IF;
END $$;

-- notifications inherits BaseEntity, so Hibernate expects a deleted_at column.
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Missing foreign keys. NOT VALID keeps startup working when legacy rows already
-- violate the constraint; new writes are still enforced.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_customer') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_customer
            FOREIGN KEY (customer_id) REFERENCES customers(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_created_by') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_created_by
            FOREIGN KEY (created_by) REFERENCES users(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_assigned_to') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_assigned_to
            FOREIGN KEY (assigned_to) REFERENCES users(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notifications_user') THEN
        ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user
            FOREIGN KEY (user_id) REFERENCES users(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stock_units_scanned_by') THEN
        ALTER TABLE stock_units ADD CONSTRAINT fk_stock_units_scanned_by
            FOREIGN KEY (scanned_by) REFERENCES users(id) NOT VALID;
    END IF;
END $$;

-- Uniqueness that the application already assumes. Both indexes are skipped
-- (with a notice) when legacy duplicates would make them fail.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'ux_payments_transaction_ref') THEN
        IF EXISTS (
            SELECT 1 FROM payments
            WHERE transaction_ref IS NOT NULL
            GROUP BY transaction_ref HAVING COUNT(*) > 1
        ) THEN
            RAISE NOTICE 'ux_payments_transaction_ref not created: duplicate transaction_ref values';
        ELSE
            CREATE UNIQUE INDEX ux_payments_transaction_ref
                ON payments(transaction_ref) WHERE transaction_ref IS NOT NULL;
        END IF;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'ux_customers_phone') THEN
        IF EXISTS (
            SELECT 1 FROM customers
            WHERE phone IS NOT NULL
            GROUP BY phone HAVING COUNT(*) > 1
        ) THEN
            RAISE NOTICE 'ux_customers_phone not created: duplicate phone values';
        ELSE
            CREATE UNIQUE INDEX ux_customers_phone
                ON customers(phone) WHERE phone IS NOT NULL;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_leads_follow_up_status ON leads(follow_up_at, status);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);