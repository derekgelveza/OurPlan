-- Sign-in support: columns the User entity needs for login and email verification.
-- V3 and V4 belong to the calendar module, so V5 is the next free version.
-- IF NOT EXISTS / the UPDATEs make this safe on a database where Hibernate (ddl-auto=update)
-- already added some of these columns.

ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_expiration TIMESTAMP;

-- Rows created before sign-in existed: email as the handle, and an empty password, which never
-- matches a BCrypt hash, so nobody can log in as them until they reset it.
UPDATE users SET username = email WHERE username IS NULL;
UPDATE users SET password = '' WHERE password IS NULL;
UPDATE users SET enabled = FALSE WHERE enabled IS NULL;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN password SET NOT NULL;
ALTER TABLE users ALTER COLUMN enabled SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
