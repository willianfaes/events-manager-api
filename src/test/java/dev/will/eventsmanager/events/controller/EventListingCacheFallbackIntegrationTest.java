package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.common.config.CacheConfig;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Redis failing is simulated with a cache that throws what a dead Redis throws (a QueryTimeoutException), so the
// test needs no real outage. Not @Transactional, same reason as EventListingCacheIntegrationTest.
class EventListingCacheFallbackIntegrationTest extends AbstractIntegrationTest {

    private static final String NAME_PREFIX = "fallback-";

    // Off while AbstractIntegrationTest clears the caches before each test, on during the test itself.
    private static final AtomicBoolean redisDown = new AtomicBoolean(false);

    @TestConfiguration
    static class FailingCacheConfig {

        // Replaces the Redis cache manager in this test context. The proxy keeps the production behaviour of
        // running evictions after the commit.
        @Bean
        CacheManager failingCacheManager() {
            Cache failing = mock(Cache.class, invocation -> {
                if (invocation.getMethod().getName().equals("getName")) {
                    return CacheConfig.EVENTS_LISTING;
                }
                if (redisDown.get()) {
                    throw new QueryTimeoutException("Redis is down");
                }
                return null;
            });
            SimpleCacheManager manager = new SimpleCacheManager();
            manager.setCaches(List.of(failing));
            manager.afterPropertiesSet();
            return new TransactionAwareCacheManagerProxy(manager);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void redisGoesDown() {
        redisDown.set(true);
    }

    @AfterEach
    void cleanUp() {
        redisDown.set(false);
        List<Long> addressIds = jdbcTemplate.queryForList(
                "SELECT address_id FROM events WHERE name LIKE ?", Long.class, NAME_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM events WHERE name LIKE ?", NAME_PREFIX + "%");
        addressIds.forEach(id -> jdbcTemplate.update("DELETE FROM event_addresses WHERE id = ?", id));
    }

    @Test
    @DisplayName("GET /events - should answer from the database when the cache fails")
    void getAllEvents_whenTheCacheFails_servesFromTheDatabase() throws Exception {
        String name = NAME_PREFIX + UUID.randomUUID();
        eventRepository.save(Event.builder()
                .name(name)
                .category(Category.CONCERT)
                .schedule(LocalDateTime.of(2126, 10, 1, 10, 0))
                .address(EventAddress.builder().street("123 Main St").city("Sample City")
                        .postalCode("12345").countryCode("US").build())
                .capacity(10)
                .build());

        String body = mockMvc.perform(get("/events").param("name", name).with(authenticatedUser()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        then(body).contains(name);
    }

    @Test
    @DisplayName("POST /admin/events - should still create the event when the cache fails")
    void createEvent_whenTheCacheFails_stillCreatesTheEvent() throws Exception {
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
}
