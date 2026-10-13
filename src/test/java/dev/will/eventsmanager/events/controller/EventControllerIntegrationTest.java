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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                .capacity(100)
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
                .capacity(100)
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

    @Test
    @DisplayName("DELETE /events/{eventId}/registrations - should return 204 and remove the active registration")
    void unregisterFromEvent_whenActiveRegistration_softDeletesIt() throws Exception {
        User user = userRepository.save(validUser());
        Event event = saveEvent();
        registrationsRepository.save(EventRegistration.builder().user(user).event(event).build());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/events/{eventId}/registrations", event.getId())
                        .with(authenticatedUser()))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        then(registrationsRepository.findByUserIdAndEventId(user.getId(), event.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/registrations - should return 404 for the registration when it was already cancelled")
    void unregisterFromEvent_whenAlreadyUnregistered_returns404() throws Exception {
        User user = userRepository.save(validUser());
        Event event = saveEvent();
        EventRegistration registration = registrationsRepository.save(
                EventRegistration.builder().user(user).event(event).build());
        registrationsRepository.delete(registration); // soft delete (@SQLDelete)
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/events/{eventId}/registrations", event.getId())
                        .with(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Registration")));
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/registrations - should return 404 for the event when it does not exist")
    void unregisterFromEvent_whenEventDoesNotExist_returns404() throws Exception {
        userRepository.save(validUser());

        mockMvc.perform(delete("/events/{eventId}/registrations", 999_999L)
                        .with(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Event")));
    }

    @Test
    @DisplayName("DELETE then POST /events/{eventId}/registrations - should revive the same registration instead of creating a new one")
    void unregisterFromEvent_thenRegisterAgain_reusesSameRegistration() throws Exception {
        User user = userRepository.save(validUser());
        Event event = saveEvent();
        Long registrationId = registrationsRepository.save(
                EventRegistration.builder().user(user).event(event).build()).getId();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/events/{eventId}/registrations", event.getId())
                        .with(authenticatedUser()))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/events/{eventId}/registrations", event.getId())
                        .with(authenticatedUser()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/registrations/" + registrationId)));
    }

    @Test
    @DisplayName("DELETE then POST /events/{eventId}/registrations - should give the freed spot to another user and keep the event full")
    void unregisterFromEvent_thenAnotherUserRegisters_takesTheFreedSpot() throws Exception {
        User holder = userRepository.save(validUser());
        userRepository.save(userNamed("Bruno", "bruno@example.com"));
        Event event = saveEvent(1);
        registrationsRepository.save(EventRegistration.builder().user(holder).event(event).build());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/events/{eventId}/registrations", event.getId())
                        .with(authenticatedUser()))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/events/{eventId}/registrations", event.getId())
                        .with(asUser("bruno@example.com")))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();

        then(registrationsRepository.countByEventId(event.getId())).isEqualTo(1);

        mockMvc.perform(post("/events/{eventId}/registrations", event.getId())
                        .with(authenticatedUser()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("full")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/events/{eventId}/registrations - should list only active registrations, sorted by user name")
    void getEventAttendees_whenOneRegistrationWasCancelled_listsOnlyActiveOnes() throws Exception {
        User bruno = userRepository.save(userNamed("Bruno", "bruno@example.com"));
        User ana = userRepository.save(userNamed("Ana", "ana@example.com"));
        User carla = userRepository.save(userNamed("Carla", "carla@example.com"));
        Event event = saveEvent();
        registrationsRepository.save(EventRegistration.builder().user(bruno).event(event).build());
        registrationsRepository.save(EventRegistration.builder().user(ana).event(event).build());
        EventRegistration cancelled = registrationsRepository.save(
                EventRegistration.builder().user(carla).event(event).build());
        registrationsRepository.delete(cancelled); // soft delete (@SQLDelete)
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/admin/events/{eventId}/registrations", event.getId()).param("sort", "NAME_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Ana"))
                .andExpect(jsonPath("$.content[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.content[1].name").value("Bruno"));
    }

    private User userNamed(String name, String email) {
        User user = validUser();
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private RequestPostProcessor asUser(String email) {
        return authentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private Event saveEvent() {
        return saveEvent(100);
    }

    private Event saveEvent(int capacity) {
        EventAddress eventAddress = EventAddress.builder()
                .street("123 Main St")
                .city("Sample City")
                .postalCode("12345")
                .countryCode("US")
                .venueName("Sample Venue")
                .build();

        return eventRepository.save(Event.builder()
                .name("Fake Event")
                .category(Category.CONCERT)
                .schedule(DEFAULT_FUTURE_SCHEDULE)
                .address(eventAddress)
                .capacity(capacity)
                .build());
    }
}
