-- The order search matches LOWER(column) LIKE '%term%', which a b-tree index
-- cannot serve. Trigram indexes can, but pg_trgm may not be installed (or the
-- role may lack the privilege), so both steps degrade to a no-op instead of
-- failing the deployment.

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'pg_trgm unavailable (%); skipping trigram indexes', SQLERRM;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        CREATE INDEX IF NOT EXISTS idx_orders_account_number_trgm
            ON orders USING gin (lower(account_number) gin_trgm_ops);

        CREATE INDEX IF NOT EXISTS idx_orders_delivery_city_trgm
            ON orders USING gin (lower(delivery_city) gin_trgm_ops);

        CREATE INDEX IF NOT EXISTS idx_orders_delivery_notes_trgm
            ON orders USING gin (lower(delivery_notes) gin_trgm_ops);

        CREATE INDEX IF NOT EXISTS idx_customers_name_trgm
            ON customers USING gin (lower(name) gin_trgm_ops);
    END IF;
END $$;
