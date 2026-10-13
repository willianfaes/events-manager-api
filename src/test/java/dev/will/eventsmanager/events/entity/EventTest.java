package dev.will.eventsmanager.events.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

class EventTest {

    @DisplayName("remainingSpots is the capacity minus the active registrations")
    @ParameterizedTest(name = "capacity {0} with {1} active registrations leaves {2}")
    @CsvSource({
            "10, 0, 10",
            "10, 3, 7",
            "10, 10, 0"
    })
    void remainingSpots(int capacity, long activeRegistrations, int expected) {
        Event event = Event.builder().capacity(capacity).build();

        int remaining = event.remainingSpots(activeRegistrations);

        then(remaining).isEqualTo(expected);
    }

    @Test
    @DisplayName("remainingSpots fails when there are more active registrations than the capacity")
    void remainingSpots_whenOverCapacity_throwsIllegalState() {
        Event event = Event.builder().id(5L).capacity(10).build();

        thenThrownBy(() -> event.remainingSpots(11))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5")
                .hasMessageContaining("10")
                .hasMessageContaining("11");
    }
}
