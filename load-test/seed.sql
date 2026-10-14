-- Data for the load test (docker profile starts empty). Run once against events_manager_db; safe to run twice.
DO $$
DECLARE
    address_id BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM events WHERE name = 'load-test-race') THEN
        RAISE NOTICE 'load test data already present, nothing to do';
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
