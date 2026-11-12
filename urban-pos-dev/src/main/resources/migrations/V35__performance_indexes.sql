-- Hot-path indexes for list, report and dashboard queries.
-- IF NOT EXISTS keeps this idempotent on databases that already carry ad-hoc indexes.

CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_created_by_created_at ON orders (created_by, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders (order_status, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_assigned_to ON orders (assigned_to);
CREATE INDEX IF NOT EXISTS idx_orders_courier_id ON orders (courier_id);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_staff_logs_timestamp ON staff_logs (timestamp);
CREATE INDEX IF NOT EXISTS idx_staff_logs_staff_id_timestamp ON staff_logs (staff_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_requisitions_status ON requisitions (status);
CREATE INDEX IF NOT EXISTS idx_requisitions_requested_by ON requisitions (requested_by);

CREATE INDEX IF NOT EXISTS idx_payments_payment_date ON payments (payment_date);

CREATE INDEX IF NOT EXISTS idx_expenses_created_at ON expenses (created_at);

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_assigned_staff_id ON products (assigned_staff_id);
