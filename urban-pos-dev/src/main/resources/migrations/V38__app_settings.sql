-- Shop-level settings entered by an admin in the UI (e.g. the M-Pesa paybill that is
-- printed on receipts and sent in SMS). Key/value so new settings need no migration.
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value TEXT,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by    UUID
);
