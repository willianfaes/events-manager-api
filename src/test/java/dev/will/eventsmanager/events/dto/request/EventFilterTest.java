package dev.will.eventsmanager.events.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.BDDAssertions.then;

class EventFilterTest {

    @Test
    @DisplayName("constructor - should default from to today when it is null")
    void constructor_should_default_from_to_today_when_null() {
        LocalDate before = LocalDate.now();

        EventFilter filter = new EventFilter(null, null, null);

        LocalDate after = LocalDate.now();
        // Bounded by two readings so the test cannot fail if it runs across midnight.
        then(filter.from()).isBetween(before, after);
    }

    @Test
    @DisplayName("constructor - should keep from when it is informed")
    void constructor_should_keep_from_when_informed() {
        LocalDate from = LocalDate.of(2050, 1, 1);

        EventFilter filter = new EventFilter(null, from, null);

        then(filter.from()).isEqualTo(from);
    }
}
