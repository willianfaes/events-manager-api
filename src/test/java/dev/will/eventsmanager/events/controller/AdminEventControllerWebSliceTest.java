package dev.will.eventsmanager.events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.will.eventsmanager.auth.config.RestAuthenticationEntryPoint;
import dev.will.eventsmanager.auth.config.SecurityConfig;
import dev.will.eventsmanager.auth.service.TokenService;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.common.pagination.AttendeeSort;
import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventAddressResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static dev.will.eventsmanager.TestFixtures.emptyPage;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminEventController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class AdminEventControllerWebSliceTest {

    private static final LocalDateTime DEFAULT_FUTURE_SCHEDULE = LocalDateTime.of(2126, 10, 1, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 201 Created with Location header when ADMIN provides valid request")
    void createEvent_should_returnCreated_when_validRequestAsAdmin() throws Exception {
        Long generatedId = 88L;
        EventCreateRequest request = validRequest().build();
        EventResponse response = validResponse().id(generatedId).build();

        given(eventService.createEvent(request)).willReturn(response);

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/events/" + generatedId))
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.id").value(generatedId))
                .andExpect(jsonPath("$.capacity").value(50));

        then(eventService).should().createEvent(request);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 400 Bad Request when capacity is missing")
    void createEvent_should_returnBadRequest_when_capacityIsMissing() throws Exception {
        EventCreateRequest request = validRequest().capacity(null).build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("capacity")));

        then(eventService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 400 Bad Request when capacity is zero or negative")
    void createEvent_should_returnBadRequest_when_capacityIsNotPositive(int invalidCapacity) throws Exception {
        EventCreateRequest request = validRequest().capacity(invalidCapacity).build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("capacity")));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 201 Created when description has exactly 1000 characters")
    void createEvent_should_returnCreated_when_descriptionIs1000Chars() throws Exception {
        Long generatedId = 105L;
        EventCreateRequest request = validRequest().description("A".repeat(1000)).build();

        given(eventService.createEvent(request)).willReturn(validResponse().id(generatedId).build());

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generatedId))
                .andExpect(header().string("Location", endsWith("/events/" + generatedId)));

        then(eventService).should().createEvent(request);
    }

    @Test
    @DisplayName("POST /admin/events - should return 401 Unauthorized when request is unauthenticated")
    void createEvent_should_returnUnauthorized_when_noAuthentication() throws Exception {
        EventCreateRequest request = validRequest().build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /admin/events - should return 403 Forbidden when user has insufficient role")
    void createEvent_should_returnForbidden_when_roleIsUser() throws Exception {
        EventCreateRequest request = validRequest().build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 400 Bad Request when category is missing")
    void createEvent_should_returnBadRequest_when_categoryIsNull() throws Exception {
        EventCreateRequest request = validRequest().category(null).build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("category")));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 400 Bad Request when schedule is in the past")
    void createEvent_should_returnBadRequest_when_scheduleIsInPast() throws Exception {
        EventCreateRequest request = validRequest().schedule(LocalDateTime.now().minusDays(1)).build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 400 Bad Request when description exceeds 1000 characters")
    void createEvent_should_returnBadRequest_when_descriptionExceedsLimit() throws Exception {
        String description1001 = "A".repeat(1001);
        EventCreateRequest request = validRequest().description(description1001).build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("description")));

        then(eventService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/events - should return 400 Bad Request when address street is blank or missing")
    void createEvent_should_returnBadRequest_when_streetIsBlankOrMissing(String invalidStreet) throws Exception {
        EventCreateRequest request = validRequest()
                .address(validAddress(invalidStreet))
                .build();

        mockMvc.perform(post("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("street")));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/events/{eventId}/registrations - should return 200 and use default pagination and NEWEST sort when ADMIN sends no params")
    void getEventAttendees_should_returnOk_when_adminWithDefaultParams() throws Exception {
        Long eventId = 10L;
        Pageable defaultPageable = PageRequest.of(0, 20, AttendeeSort.NEWEST.toSort());
        given(eventService.findEventAttendees(eventId, defaultPageable)).willReturn(emptyPage());

        mockMvc.perform(get("/admin/events/{eventId}/registrations", eventId))
                .andExpect(status().isOk());

        then(eventService).should().findEventAttendees(eventId, defaultPageable);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /admin/events/{eventId}/registrations - should return 403 Forbidden when user has insufficient role")
    void getEventAttendees_should_returnForbidden_when_roleIsUser() throws Exception {
        mockMvc.perform(get("/admin/events/{eventId}/registrations", 10L))
                .andExpect(status().isForbidden());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/events/{eventId}/registrations - should return 404 Not Found when event does not exist")
    void getEventAttendees_should_returnNotFound_when_eventDoesNotExist() throws Exception {
        Long eventId = 99L;
        Pageable defaultPageable = PageRequest.of(0, 20, AttendeeSort.NEWEST.toSort());
        given(eventService.findEventAttendees(eventId, defaultPageable))
                .willThrow(new ResourceNotFoundException("Event", eventId));

        mockMvc.perform(get("/admin/events/{eventId}/registrations", eventId))
                .andExpect(status().isNotFound());
    }

    private EventCreateRequest.EventCreateRequestBuilder validRequest() {
        return EventCreateRequest.builder()
                .name("Summit 2027")
                .category(Category.PARTY)
                .schedule(DEFAULT_FUTURE_SCHEDULE)
                .address(validAddress())
                .description("Valid description")
                .capacity(50);
    }

    private EventAddressRequest validAddress() {
        return validAddress("Main St");
    }

    private EventAddressRequest validAddress(String street) {
        return EventAddressRequest.builder()
                .street(street)
                .complement("Complement")
                .city("City")
                .stateOrProvince("SP")
                .postalCode("12345")
                .countryCode("BR")
                .venueName("Venue")
                .build();
    }

    private EventResponse.EventResponseBuilder validResponse() {
        return EventResponse.builder()
                .id(1L)
                .name("Event Name")
                .category(Category.PARTY)
                .schedule(DEFAULT_FUTURE_SCHEDULE)
                .address(validAddressResponse())
                .description("Response description")
                .capacity(50);
    }

    private EventAddressResponse validAddressResponse() {
        return EventAddressResponse.builder()
                .id(1L)
                .street("Av. Response 100")
                .complement("Rooftop")
                .city("Sao Paulo")
                .stateOrProvince("SP")
                .postalCode("01000-000")
                .countryCode("BR")
                .venueName("Skyline Lounge")
                .build();
    }
}
