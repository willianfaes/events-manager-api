-- The identity provider owns the identity: external_id is the token's `sub`, and the e-mail and password live there.
-- These changes only widen what the table accepts, so the code that still writes them keeps working until V4 drops
-- the columns.
ALTER TABLE users
    ADD COLUMN external_id VARCHAR(255) UNIQUE;

ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL;

ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;
