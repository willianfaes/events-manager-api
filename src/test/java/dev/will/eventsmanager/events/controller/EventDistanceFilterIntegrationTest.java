package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static dev.will.eventsmanager.TestFixtures.authenticatedAs;
import static dev.will.eventsmanager.TestFixtures.subOf;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class EventDistanceFilterIntegrationTest extends AbstractIntegrationTest {

    private static final Coordinates MILANO = new Coordinates(45.4642, 9.19);
    private static final Coordinates VERONA = new Coordinates(45.4380, 10.9938);
    private static final Coordinates ROMA = new Coordinates(41.9028, 12.4964);
    private static final String SUB = subOf("distance-filter");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    UserRepository userRepository;

    // The seeded events have coordinates too, so each test isolates its own events by name.
    private final String token = "distance-" + UUID.randomUUID();

    @Test
    @DisplayName("GET /events - should return only the events inside maxDistanceKm, each with its distance")
    void getAllEvents_whenFilteredByDistance_keepsTheEventsInsideIt() throws Exception {
        saveProfile(MILANO);
        saveEvent(token + "-verona", VERONA);
        saveEvent(token + "-roma", ROMA);

        mockMvc.perform(get("/events").param("name", token).param("maxDistanceKm", "200")
                        .with(authenticatedAs(SUB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value(token + "-verona"))
                .andExpect(jsonPath("$.content[0].distanceKm", closeTo(MILANO.distanceKmTo(VERONA), 0.1)));
    }

    @Test
    @DisplayName("GET /events - should keep working with no profile and hide the distance when there is no filter")
    void getAllEvents_withoutTheFilter_behavesAsBefore() throws Exception {
        saveEvent(token, VERONA);

        mockMvc.perform(get("/events").param("name", token).with(authenticatedAs(SUB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].distanceKm").doesNotExist());
    }

    @Test
    @DisplayName("GET /events - should return 404 when the filter is used without a profile")
    void getAllEvents_whenFilteredWithoutAProfile_returnsNotFound() throws Exception {
        mockMvc.perform(get("/events").param("maxDistanceKm", "200").with(authenticatedAs(SUB)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /events - should return 409 when the profile has no coordinates")
    void getAllEvents_whenTheProfileHasNoCoordinates_returnsConflict() throws Exception {
        saveProfile(null);

        mockMvc.perform(get("/events").param("maxDistanceKm", "200").with(authenticatedAs(SUB)))
                .andExpect(status().isConflict());
    }

    private void saveProfile(Coordinates coordinates) {
        User profile = validUser();
        profile.setExternalId(SUB);
        profile.getAddress().locateAt(coordinates);
        userRepository.save(profile);
    }

    private void saveEvent(String name, Coordinates coordinates) {
        EventAddress address = EventAddress.builder()
                .street("Any Street 1")
                .city("Any City")
                .postalCode("10100")
                .countryCode("IT")
                .build();
        address.locateAt(coordinates);
        eventRepository.save(Event.builder()
                .name(name)
                .category(Category.CONCERT)
                .schedule(LocalDateTime.of(2126, 10, 1, 20, 0))
                .address(address)
                .capacity(10)
                .build());
    }
}
