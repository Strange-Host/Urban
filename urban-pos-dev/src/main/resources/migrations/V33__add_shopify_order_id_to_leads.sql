ALTER TABLE leads ADD COLUMN IF NOT EXISTS shopify_order_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_leads_shopify_order_id ON leads(shopify_order_id);