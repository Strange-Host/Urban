-- Remove old column no longer used
ALTER TABLE leads
DROP COLUMN IF EXISTS extra_details;

-- Add new dynamic support columns
ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS form_id INTEGER;

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS form_name VARCHAR(255);

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS raw_data TEXT;

-- Make rigid columns flexible for multiple forms
ALTER TABLE leads ALTER COLUMN name DROP NOT NULL;
ALTER TABLE leads ALTER COLUMN location DROP NOT NULL;
ALTER TABLE leads ALTER COLUMN phone_number DROP NOT NULL;