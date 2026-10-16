package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The shared Redis container is paused during each test: it stops answering without closing the connection,
// so the short Redis timeouts are what keep the requests fast. Not @Transactional, same reason as
// EventListingCacheIntegrationTest.
class EventListingCacheFallbackIntegrationTest extends AbstractIntegrationTest {

    private static final String NAME_PREFIX = "fallback-";

    @BeforeEach
    void pauseRedis() {
        redis.getDockerClient().pauseContainerCmd(redis.getContainerId()).exec();
    }

    @AfterEach
    void resumeRedis() {
        redis.getDockerClient().unpauseContainerCmd(redis.getContainerId()).exec();
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoSpyBean
    EventRepository eventRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        List<Long> addressIds = jdbcTemplate.queryForList(
                "SELECT address_id FROM events WHERE name LIKE ?", Long.class, NAME_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM events WHERE name LIKE ?", NAME_PREFIX + "%");
        addressIds.forEach(id -> jdbcTemplate.update("DELETE FROM event_addresses WHERE id = ?", id));
    }

    @Test
    @DisplayName("GET /events - should answer from the database, and quickly, when Redis is down")
    void getAllEvents_whenRedisIsDown_servesFromTheDatabase() throws Exception {
        String name = NAME_PREFIX + UUID.randomUUID();
        eventRepository.save(Event.builder()
                .name(name)
                .category(Category.CONCERT)
                .schedule(LocalDateTime.of(2126, 10, 1, 10, 0))
                .address(EventAddress.builder().street("123 Main St").city("Sample City")
                        .postalCode("12345").countryCode("US").build())
                .capacity(10)
                .build());

        long start = System.nanoTime();
        String first = mockMvc.perform(get("/events").param("name", name).with(authenticatedUser()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        mockMvc.perform(get("/events").param("name", name).with(authenticatedUser()))
                .andExpect(status().isOk());

        then(first).contains(name);
        then(elapsed).isLessThan(Duration.ofSeconds(3));
        // Nothing was cached, so each call went to the database.
        verify(eventRepository, times(2)).findAll(anySpecification(), ArgumentMatchers.<Pageable>any());
    }

    @Test
    @DisplayName("POST /admin/events - should still create the event when Redis is down")
    void createEvent_whenRedisIsDown_stillCreatesTheEvent() throws Exception {
        mockMvc.perform(post("/admin/events")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "category": "CONCERT",
                                  "schedule": "2126-10-01T10:00:00",
                                  "address": {"street": "123 Main St", "city": "Sample City",
                                              "postalCode": "12345", "countryCode": "US"},
                                  "capacity": 10
                                }
                                """.formatted(NAME_PREFIX + UUID.randomUUID())))
                .andExpect(status().isCreated());
    }

    private static Specification<Event> anySpecification() {
        return ArgumentMatchers.any();
    }
}
