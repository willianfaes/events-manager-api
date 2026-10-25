-- Cognito owns the identity (e-mail, password) and the access (groups), so the profile keeps only its own data,
-- linked to the token's `sub`. Fails on purpose if a user has no external_id instead of deleting it.
ALTER TABLE users
    DROP COLUMN password,
    DROP COLUMN email,
    DROP COLUMN role;

ALTER TABLE users
    ALTER COLUMN external_id SET NOT NULL;
