-- Data for the load test (docker profile starts empty). Run it against events_manager_db before each session:
-- the first run creates the events, later runs move them forward again, so the race never falls in the past
-- and the listing keeps the same 1,001 upcoming events.
DO $$
DECLARE
    address_id BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM events WHERE name = 'load-test-race') THEN
        -- Same days ahead as the insert below: i is 0 for the race and the suffix of load-test-event-<i>.
        UPDATE events
        SET schedule   = now() + ((1 + CASE WHEN name = 'load-test-race' THEN 0
                                            ELSE substring(name FROM 'load-test-event-(\d+)$')::INT END % 90) || ' days')::interval,
            updated_at = now()
        WHERE name = 'load-test-race' OR name ~ '^load-test-event-\d+$';
        RAISE NOTICE 'load test data already present, events moved forward';
        RETURN;
    END IF;

    FOR i IN 0..1000 LOOP
        INSERT INTO event_addresses (street, city, state_or_province, postal_code, country_code, venue_name)
        VALUES ('Via del Corso ' || i, 'Rome', 'Lazio', '00186', 'IT', 'Venue ' || i)
        RETURNING id INTO address_id;

        INSERT INTO events (name, address_id, category, schedule, description, capacity)
        VALUES (
            CASE WHEN i = 0 THEN 'load-test-race' ELSE 'load-test-event-' || i END,
            address_id,
            (ARRAY['PARTY', 'SPORT', 'CONCERT', 'CULTURAL'])[1 + i % 4],
            now() + ((1 + i % 90) || ' days')::interval,
            'Seeded for the load test',
            CASE WHEN i = 0 THEN 50 ELSE 20 + i % 180 END
        );
    END LOOP;
END $$;
