ALTER TABLE users
    DROP CONSTRAINT uk_users_phone_number;

ALTER TABLE users
    DROP COLUMN phone_number;
