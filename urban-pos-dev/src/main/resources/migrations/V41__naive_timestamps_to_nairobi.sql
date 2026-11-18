-- The application now runs in Africa/Nairobi (UTC+3, no daylight saving) instead of UTC.
--
-- TIMESTAMPTZ columns hold real instants, so they need no change: they are simply read back
-- in the new zone. The plain TIMESTAMP columns below hold whatever wall-clock time the JVM
-- wrote, which was UTC until now. Left alone, every existing value would be read as Nairobi
-- time and be three hours too early (a lead created at 09:00 Nairobi would show as 06:00, and
-- an unsent follow-up reminder would fire three hours late).
--
-- This migration moves those values forward by three hours so they match the new zone. It must
-- ship in the same release that switches the application zone, and the old (UTC) instances
-- must be stopped before it runs, otherwise rows written by them afterwards stay unshifted.
-- NULLs are unchanged by the arithmetic.

UPDATE customers      SET created_at = created_at + INTERVAL '3 hours',
                          updated_at = updated_at + INTERVAL '3 hours',
                          deleted_at = deleted_at + INTERVAL '3 hours';

UPDATE leads          SET created      = created      + INTERVAL '3 hours',
                          follow_up_at = follow_up_at + INTERVAL '3 hours';

UPDATE users          SET refresh_token_expiry = refresh_token_expiry + INTERVAL '3 hours',
                          last_login_at        = last_login_at        + INTERVAL '3 hours',
                          last_logout_at       = last_logout_at       + INTERVAL '3 hours';

UPDATE user_permissions SET created_at = created_at + INTERVAL '3 hours';

UPDATE staff_logs     SET "timestamp" = "timestamp" + INTERVAL '3 hours';

UPDATE logout_checks  SET created_at = created_at + INTERVAL '3 hours';

UPDATE notifications  SET sent_at    = sent_at    + INTERVAL '3 hours',
                          created_at = created_at + INTERVAL '3 hours',
                          updated_at = updated_at + INTERVAL '3 hours',
                          deleted_at = deleted_at + INTERVAL '3 hours';

UPDATE couriers       SET created_at = created_at + INTERVAL '3 hours',
                          updated_at = updated_at + INTERVAL '3 hours',
                          deleted_at = deleted_at + INTERVAL '3 hours';

UPDATE orders         SET courier_assigned_at = courier_assigned_at + INTERVAL '3 hours';
