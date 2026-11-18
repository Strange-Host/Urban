-- Managers now get lead assignment, courier viewing and staff logs by default. Existing managers
-- only receive the ones they have no row for; a row an admin explicitly set (granted or revoked)
-- is left exactly as it is.
INSERT INTO user_permissions (user_id, permission, granted)
SELECT u.id, p.permission, TRUE
FROM users u
CROSS JOIN (VALUES ('CAN_ASSIGN_LEADS'), ('CAN_VIEW_COURIERS'), ('CAN_VIEW_STAFF_LOGS')) AS p(permission)
WHERE u.role = 'MANAGER'
  AND u.deleted_at IS NULL
ON CONFLICT (user_id, permission) DO NOTHING;
