ALTER TABLE events
    ADD COLUMN capacity INT NOT NULL CHECK (capacity > 0);
