-- Runs only when the postgres_data volume is created (docker-entrypoint-initdb.d), as POSTGRES_USER, who owns it.
-- The notification service keeps its own database in this container and logs in with the same user.
-- On an existing volume, create it once by hand: docker compose exec db createdb -U <DB_USER> events_notification_db
CREATE DATABASE events_notification_db;
