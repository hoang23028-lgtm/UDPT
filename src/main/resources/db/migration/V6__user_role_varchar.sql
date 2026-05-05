-- Longer role names (e.g. MODERATOR) without truncation; existing USER/ADMIN values unchanged.
ALTER TABLE app_users ALTER COLUMN role TYPE VARCHAR(32);
