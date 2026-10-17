package dev.will.eventsmanager.events.controller;

import com.jayway.jsonpath.JsonPath;
import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Not @Transactional on purpose: the cache manager is transaction aware, so inside a test that rolls back
// nothing would ever be written to Redis. Data is committed and removed in cleanUp().
class EventListingCacheIntegrationTest extends AbstractIntegrationTest {

    private static final String CACHE_NAME = "events-listing";
    private static final String EMAIL_PREFIX = "listing-cache-";
    private static final LocalDateTime FUTURE_SCHEDULE = LocalDateTime.of(2126, 10, 1, 10, 0);

    @Autowired
    MockMvc mockMvc;

    @MockitoSpyBean
    EventRepository eventRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StringRedisTemplate redisTemplate;

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

    @Test
    @DisplayName("GET /events - should answer the same listing for another user from the cache, without querying the database")
    void getAllEvents_whenAnotherUserAsksTheSameListing_servesItFromTheCache() throws Exception {
        String token = uniqueToken();
        saveEvent(token, 10);

        String forFirstUser = listing(asEmail("first@example.com"), get("/events").param("name", token));
        String forSecondUser = listing(asEmail("second@example.com"), get("/events").param("name", token));

        verify(eventRepository, times(1)).findAll(anySpecification(), ArgumentMatchers.<Pageable>any());
        then(forSecondUser).isEqualTo(forFirstUser);
        then(forSecondUser).contains(token);
    }

    @ParameterizedTest(name = "{0}={1} should not reuse the entry of the base listing")
    @CsvSource({"sort, DATE_DESC", "size, 5", "page, 1", "from, 2126-01-01", "to, 2126-12-31", "name, cache-"})
    @DisplayName("GET /events - should cache a listing apart when any parameter of the key changes")
    void getAllEvents_whenAParameterChanges_queriesTheDatabaseAgain(String param, String value) throws Exception {
        String token = uniqueToken();
        saveEvent(token, 10);
        listing(asEmail("user@example.com"), get("/events").param("name", token));
        listing(asEmail("user@example.com"), get("/events").param("name", token));
        verify(eventRepository, times(1)).findAll(anySpecification(), ArgumentMatchers.<Pageable>any());

        listing(asEmail("user@example.com"), get("/events").param("name", token).param(param, value));

        verify(eventRepository, times(2)).findAll(anySpecification(), ArgumentMatchers.<Pageable>any());
    }

    @Test
    @DisplayName("GET /events - should store the cached listing with a TTL of at most 30 seconds")
    void getAllEvents_whenCached_expiresWithinThirtySeconds() throws Exception {
        String token = uniqueToken();
        saveEvent(token, 10);

        listing(asEmail("user@example.com"), get("/events").param("name", token));

        Set<String> keys = redisTemplate.keys(CACHE_NAME + "::*");
        then(keys).isNotEmpty();
        for (String key : keys) {
            then(redisTemplate.getExpire(key)).isBetween(1L, 30L);
        }
    }

    @Test
    @DisplayName("GET /events - should keep the old remainingSpots after a registration until the cache expires")
    void getAllEvents_whenSomeoneRegisters_keepsRemainingSpotsUntilTheCacheExpires() throws Exception {
        String token = uniqueToken();
        Event event = saveEvent(token, 5);
        RequestPostProcessor user = asEmail(saveUser().getEmail());
        listing(user, get("/events").param("name", token));

        mockMvc.perform(post("/events/{eventId}/registrations", event.getId()).with(user))
                .andExpect(status().isCreated());

        then(listing(user, get("/events").param("name", token))).contains("\"remainingSpots\":5");
    }

    @Test
    @DisplayName("POST /admin/events - should show the new event in the next listing")
    void createEvent_whenListingIsCached_nextListingShowsTheNewEvent() throws Exception {
        String token = uniqueToken();
        saveEvent(token, 10);
        listing(asEmail("user@example.com"), get("/events").param("name", token));

        String created = mockMvc.perform(post("/admin/events").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(token + "-created")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        eventIds.add(((Number) JsonPath.read(created, "$.id")).longValue());

        then(listing(asEmail("user@example.com"), get("/events").param("name", token))).contains("-created");
    }

    @Test
    @DisplayName("PUT /admin/events/{eventId} - should show the updated event in the next listing")
    void updateEvent_whenListingIsCached_nextListingShowsTheChange() throws Exception {
        String token = uniqueToken();
        Event event = saveEvent(token, 10);
        listing(asEmail("user@example.com"), get("/events").param("name", token));

        mockMvc.perform(put("/admin/events/{eventId}", event.getId()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(token + "-renamed")))
                .andExpect(status().isOk());

        then(listing(asEmail("user@example.com"), get("/events").param("name", token))).contains("-renamed");
    }

    @Test
    @DisplayName("DELETE /admin/events/{eventId} - should drop the deleted event from the next listing")
    void deleteEvent_whenListingIsCached_nextListingDropsTheEvent() throws Exception {
        String token = uniqueToken();
        Event event = saveEvent(token, 10);
        listing(asEmail("user@example.com"), get("/events").param("name", token));

        mockMvc.perform(delete("/admin/events/{eventId}", event.getId()).with(asAdmin()))
                .andExpect(status().isNoContent());

        then(listing(asEmail("user@example.com"), get("/events").param("name", token))).doesNotContain(token);
    }

    private String listing(RequestPostProcessor principal, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(principal))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // Every test filters by its own name, so the events the dev seeder inserts never interfere.
    private String uniqueToken() {
        return "cache-" + UUID.randomUUID();
    }

    private Event saveEvent(String name, int capacity) {
        EventAddress address = EventAddress.builder()
                .street("123 Main St")
                .city("Sample City")
                .postalCode("12345")
                .countryCode("US")
                .build();

        Event event = eventRepository.save(Event.builder()
                .name(name)
                .category(Category.CONCERT)
                .schedule(FUTURE_SCHEDULE)
                .address(address)
                .capacity(capacity)
                .build());
        eventIds.add(event.getId());
        return event;
    }

    private User saveUser() {
        User user = validUser();
        user.setEmail(EMAIL_PREFIX + UUID.randomUUID() + "@example.com");
        return userRepository.save(user);
    }

    private RequestPostProcessor asEmail(String email) {
        return authentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private String eventBody(String name) {
        return """
                {
                  "name": "%s",
                  "category": "CONCERT",
                  "schedule": "2126-10-01T10:00:00",
                  "address": {
                    "street": "123 Main St",
                    "city": "Sample City",
                    "postalCode": "12345",
                    "countryCode": "US"
                  },
                  "capacity": 10
                }
                """.formatted(name);
    }

    private static Specification<Event> anySpecification() {
        return ArgumentMatchers.any();
    }
}
