package dev.will.eventsmanager.users.controller;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class UserControllerIntegrationTest extends AbstractIntegrationTest  {
    private static final LocalDateTime DEFAULT_FUTURE_SCHEDULE = LocalDateTime.of(2126, 10, 1, 10, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventRegistrationsRepository registrationsRepository;


    @Test
    @DisplayName("GET /users/me/registrations - should not return cancelled registrations")
    void getUserRegisteredEvents_shouldExcludeCancelledRegistrations() throws Exception {
        User user = userRepository.save(validUser());
        Event activeEvent = saveEvent("Active Event");
        Event cancelledEvent = saveEvent("Cancelled Event");

        EventRegistration active = registrationsRepository.save(
                EventRegistration.builder().user(user).event(activeEvent).build());
        EventRegistration cancelled = registrationsRepository.save(
                EventRegistration.builder().user(user).event(cancelledEvent).build());

        registrationsRepository.delete(cancelled); // soft delete (@SQLDelete): UPDATE is_deleted = true
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/users/me/registrations").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].registrationId").value(active.getId()));
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should return only the active registrations of the logged user")
    void getRegisteredEventIds_shouldReturnOnlyTheLoggedUsersActiveRegistrations() throws Exception {
        User user = userRepository.save(validUser());
        User other = validUser();
        other.setEmail("other@example.com");
        other = userRepository.save(other);

        Event registered = saveEvent("Registered");
        Event cancelled = saveEvent("Cancelled");
        Event notRegistered = saveEvent("Not registered");
        Event onlyOther = saveEvent("Only the other user");

        registrationsRepository.save(EventRegistration.builder().user(user).event(registered).build());
        EventRegistration cancelledRegistration = registrationsRepository.save(
                EventRegistration.builder().user(user).event(cancelled).build());
        registrationsRepository.save(EventRegistration.builder().user(other).event(onlyOther).build());

        registrationsRepository.delete(cancelledRegistration); // soft delete (@SQLDelete)
        entityManager.flush();
        entityManager.clear();

        String ids = String.join(",",
                registered.getId().toString(), cancelled.getId().toString(), notRegistered.getId().toString(),
                onlyOther.getId().toString(), "999999", registered.getId().toString());

        mockMvc.perform(get("/users/me/registrations/events").param("eventIds", ids)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventIds.length()").value(1))
                .andExpect(jsonPath("$.eventIds[0]").value(registered.getId()));
    }

    private Event saveEvent(String name) {
        EventAddress address = EventAddress.builder()
                .street("123 Main St")
                .city("Sample City")
                .postalCode("12345")
                .countryCode("US")
                .venueName("Sample Venue")
                .build();

        return eventRepository.save(Event.builder()
                .name(name)
                .category(Category.CONCERT)
                .schedule(DEFAULT_FUTURE_SCHEDULE)
                .address(address)
                .capacity(100)
                .build());
    }

}
