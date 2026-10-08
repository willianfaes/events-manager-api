package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class EventControllerIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDateTime DEFAULT_FUTURE_SCHEDULE = LocalDateTime.of(2126, 10, 1, 10, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EventRegistrationsRepository registrationsRepository;

    @Test
    @WithMockUser
    void getEventById_whenEventExists_returnsEventWithAddress() throws Exception {

        EventAddress address = EventAddress.builder()
                .street("123 Main St")
                .complement("Suite 100")
                .city("Sample City")
                .stateOrProvince("State")
                .postalCode("12345")
                .countryCode("US")
                .venueName("Sample Venue")
                .build();

        Event event = Event.builder()
                .name("Fake Event")
                .category(Category.CONCERT)
                .schedule(LocalDateTime.of(2026, 9, 18, 20, 0))
                .address(address)
                .build();

        Event saved = eventRepository.save(event);
        entityManager.flush();  // grava no banco
        entityManager.clear();  // esvazia o cache: o GET vai ler do banco de verdade

        mockMvc.perform(get("/events/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.address.id").value(saved.getAddress().getId()));
    }

    @Test
    @WithMockUser
    void getEventById_whenEventDoesNotExist_returns404() throws Exception {
        mockMvc.perform(get("/events/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Event")));
    }

    @Test
    @DisplayName("POST /events/{eventId}/registrations - should revive a cancelled registration instead of creating a new one")
    void registerForEvent_whenPreviouslyCancelled_revivesSameRegistration() throws Exception {
        User user = userRepository.save(validUser());

        EventAddress eventAddress = EventAddress.builder()
                .street("123 Main St")
                .city("Sample City")
                .postalCode("12345")
                .countryCode("US")
                .venueName("Sample Venue")
                .build();

        Event event = eventRepository.save(Event.builder()
                .name("Fake Event")
                .category(Category.CONCERT)
                .schedule(DEFAULT_FUTURE_SCHEDULE)
                .address(eventAddress)
                .build());

        EventRegistration registration = registrationsRepository.save(
                EventRegistration.builder().user(user).event(event).build());

        Long registrationId = registration.getId();

        registrationsRepository.delete(registration); // soft delete (@SQLDelete): UPDATE is_deleted = true
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/events/{eventId}/registrations", event.getId())
                        .with(authenticatedUser()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/registrations/" + registrationId)));
    }
}
