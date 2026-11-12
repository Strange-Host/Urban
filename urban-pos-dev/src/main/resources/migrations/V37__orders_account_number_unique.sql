-- Account numbers are handed to customers as payment references (KCB BusinessKey).
-- They must be unique; the application only checks before insert, which races.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'ux_orders_account_number') THEN
        IF EXISTS (
            SELECT 1 FROM orders
            WHERE account_number IS NOT NULL
            GROUP BY account_number HAVING COUNT(*) > 1
        ) THEN
            RAISE NOTICE 'ux_orders_account_number not created: duplicate account_number values';
        ELSE
            CREATE UNIQUE INDEX ux_orders_account_number
                ON orders(account_number) WHERE account_number IS NOT NULL;
        END IF;
    END IF;
END $$;
