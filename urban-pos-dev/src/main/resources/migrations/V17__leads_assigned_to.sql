ALTER TABLE leads
ALTER COLUMN assigned_to TYPE uuid
USING assigned_to::uuid;