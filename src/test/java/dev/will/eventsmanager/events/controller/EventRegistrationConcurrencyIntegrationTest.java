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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static dev.will.eventsmanager.TestFixtures.authenticatedAs;
import static dev.will.eventsmanager.TestFixtures.subOf;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Not @Transactional on purpose: each request runs in its own thread and transaction,
// so the data has to be committed. Rows are removed in cleanUp().
class EventRegistrationConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL_PREFIX = "concurrency-";
    private static final LocalDateTime FUTURE_SCHEDULE = LocalDateTime.of(2126, 10, 1, 10, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EventRegistrationsRepository registrationsRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final List<Long> eventIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long eventId : eventIds) {
            Long addressId = jdbcTemplate.queryForObject(
                    "SELECT address_id FROM events WHERE id = ?", Long.class, eventId);
            jdbcTemplate.update("DELETE FROM event_registrations WHERE event_id = ?", eventId);
            jdbcTemplate.update("DELETE FROM events WHERE id = ?", eventId);
            jdbcTemplate.update("DELETE FROM event_addresses WHERE id = ?", addressId);
        }
        List<Long> userAddressIds = jdbcTemplate.queryForList(
                "SELECT address_id FROM users WHERE email LIKE ?", Long.class, EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", EMAIL_PREFIX + "%");
        userAddressIds.forEach(id -> jdbcTemplate.update("DELETE FROM user_addresses WHERE id = ?", id));
    }

    @RepeatedTest(5)
    @DisplayName("POST /events/{eventId}/registrations - should give the only free spot to exactly one of 10 concurrent users")
    void registerForEvent_whenTenUsersCompeteForOneSpot_onlyOneGetsIt() throws Exception {
        Event event = saveEventWithOneSpot();
        Long eventId = event.getId();
        List<User> users = saveUsers(10);

        List<Callable<Integer>> requests = users.stream()
                .<Callable<Integer>>map(user -> () -> register(user, eventId))
                .toList();
        List<Integer> statuses = runConcurrently(requests);

        then(statuses).filteredOn(status -> status == 201).hasSize(1);
        then(statuses).filteredOn(status -> status == 409).hasSize(9);
        then(registrationsRepository.countByEventId(eventId)).isEqualTo(1);
    }

    @RepeatedTest(5)
    @DisplayName("POST and DELETE /events/{eventId}/registrations - should never exceed capacity when a spot is freed and taken at the same time")
    void registerAndCancel_whenAtTheSameTime_neverExceedCapacity() throws Exception {
        Event event = saveEventWithOneSpot();
        Long eventId = event.getId();
        List<User> users = saveUsers(2);
        User holder = users.get(0);
        User newcomer = users.get(1);
        registrationsRepository.save(EventRegistration.builder().user(holder).event(event).build());

        List<Integer> statuses = runConcurrently(List.of(
                () -> cancel(holder, eventId),
                () -> register(newcomer, eventId)));
        int cancelStatus = statuses.get(0);
        int registerStatus = statuses.get(1);

        then(cancelStatus).isEqualTo(204);
        then(registerStatus).isIn(201, 409);
        long active = registrationsRepository.countByEventId(eventId);
        then(active).isLessThanOrEqualTo(1);
        then(active).isEqualTo(registerStatus == 201 ? 1 : 0);
    }

    private Event saveEventWithOneSpot() {
        EventAddress address = EventAddress.builder()
                .street("123 Main St")
                .city("Sample City")
                .postalCode("12345")
                .countryCode("US")
                .build();

        Event event = eventRepository.save(Event.builder()
                .name("Concurrency Event " + UUID.randomUUID())
                .category(Category.CONCERT)
                .schedule(FUTURE_SCHEDULE)
                .address(address)
                .capacity(1)
                .build());
        eventIds.add(event.getId());
        return event;
    }

    private List<User> saveUsers(int amount) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            User user = validUser();
            user.setEmail(EMAIL_PREFIX + UUID.randomUUID() + "@example.com");
            user.setExternalId(subOf(user.getEmail()));
            users.add(userRepository.save(user));
        }
        return users;
    }

    private List<Integer> runConcurrently(List<Callable<Integer>> requests) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = requests.stream()
                    .map(request -> executor.submit(() -> {
                        start.await();
                        return request.call();
                    }))
                    .toList();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }

    private int register(User user, Long eventId) throws Exception {
        return mockMvc.perform(post("/events/{eventId}/registrations", eventId).with(asUser(user)))
                .andReturn().getResponse().getStatus();
    }

    private int cancel(User user, Long eventId) throws Exception {
        return mockMvc.perform(delete("/events/{eventId}/registrations", eventId).with(asUser(user)))
                .andReturn().getResponse().getStatus();
    }

    private RequestPostProcessor asUser(User user) {
        return authenticatedAs(user.getExternalId());
    }
}
