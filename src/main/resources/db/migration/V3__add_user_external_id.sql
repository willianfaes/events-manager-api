-- The identity provider owns the identity: external_id is the token's `sub`. Both changes only widen what the table
-- accepts, so the code that still writes a password keeps working until V4 drops the column.
ALTER TABLE users
    ADD COLUMN external_id VARCHAR(255) UNIQUE;

ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL;
