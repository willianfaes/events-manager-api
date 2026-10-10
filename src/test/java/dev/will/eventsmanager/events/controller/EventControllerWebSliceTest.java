package dev.will.eventsmanager.events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.will.eventsmanager.auth.config.RestAuthenticationEntryPoint;
import dev.will.eventsmanager.auth.config.SecurityConfig;
import dev.will.eventsmanager.auth.service.TokenService;
import dev.will.eventsmanager.common.exception.AlreadyRegisteredException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.common.pagination.AttendeeSort;
import dev.will.eventsmanager.common.pagination.EventSort;
import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.dto.response.EventAddressResponse;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.emptyPage;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class EventControllerWebSliceTest {

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
    @DisplayName("createEvent should return 201 Created with Location header when ADMIN provides valid request")
    void createEvent_should_returnCreated_when_validRequestAsAdmin() throws Exception {
        Long generatedId = 88L;
        EventCreateRequest request = validRequest().build();
        EventResponse response = validResponse().id(generatedId).build();

        given(eventService.createEvent(request)).willReturn(response);

        mockMvc.perform(post("/events")
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
    @DisplayName("POST /events - should return 400 Bad Request when capacity is missing")
    void createEvent_should_returnBadRequest_when_capacityIsMissing() throws Exception {
        EventCreateRequest request = validRequest().capacity(null).build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("capacity")));

        then(eventService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /events - should return 400 Bad Request when capacity is zero or negative")
    void createEvent_should_returnBadRequest_when_capacityIsNotPositive(int invalidCapacity) throws Exception {
        EventCreateRequest request = validRequest().capacity(invalidCapacity).build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("capacity")));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 201 Created when description has exactly 1000 characters")
    void createEvent_should_returnCreated_when_descriptionIs1000Chars() throws Exception {
        Long generatedId = 105L;
        EventCreateRequest request = validRequest().description("A".repeat(1000)).build();

        given(eventService.createEvent(request)).willReturn(validResponse().id(generatedId).build());

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generatedId))
                .andExpect(header().string("Location", endsWith("/events/" + generatedId)));

        then(eventService).should().createEvent(request);
    }

    @Test
    @DisplayName("createEvent should return 401 Unauthorized when request is unauthenticated")
    void createEvent_should_returnUnauthorized_when_noAuthentication() throws Exception {
        EventCreateRequest request = validRequest().build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("createEvent should return 403 Forbidden when user has insufficient role")
    void createEvent_should_returnForbidden_when_roleIsUser() throws Exception {
        EventCreateRequest request = validRequest().build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 400 Bad Request when category is missing")
    void createEvent_should_returnBadRequest_when_categoryIsNull() throws Exception {
        EventCreateRequest request = validRequest().category(null).build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("category")));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 400 Bad Request when schedule is in the past")
    void createEvent_should_returnBadRequest_when_scheduleIsInPast() throws Exception {
        EventCreateRequest request = validRequest().schedule(LocalDateTime.now().minusDays(1)).build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 400 Bad Request when description exceeds 1000 characters")
    void createEvent_should_returnBadRequest_when_descriptionExceedsLimit() throws Exception {
        String description1001 = "A".repeat(1001);
        EventCreateRequest request = validRequest().description(description1001).build();

        mockMvc.perform(post("/events")
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
    @DisplayName("createEvent should return 400 Bad Request when address street is blank or missing")
    void createEvent_should_returnBadRequest_when_streetIsBlankOrMissing(String invalidStreet) throws Exception {
        EventCreateRequest request = validRequest()
                .address(validAddress(invalidStreet))
                .build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("street")));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /events/{eventId}/registrations - should return 201 Created and registration id when authenticated user registers for existing event")
    void registerForEvent_should_returnCreated_when_validRequestAsUser() throws Exception {
        Long eventId = 10L;
        Long registrationId = 1L;
        EventRegistrationResponse expectedResponse = new EventRegistrationResponse(registrationId);

        given(eventService.registerUserForEvent(anyString(), eq(eventId))).willReturn(expectedResponse);

        mockMvc.perform(post("/events/{eventId}/registrations", eventId)
                .with(authenticatedUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(registrationId))
                .andExpect(header().string("Location", endsWith("/events/" + eventId + "/registrations/" + registrationId)));


        then(eventService).should().registerUserForEvent("user@example.com", eventId);
    }

    @Test
    @DisplayName("POST /events/{eventId}/registrations - should return 409 Conflict when user is already registered for event")
    void registerForEvent_should_returnConflict_when_alreadyRegistered() throws Exception {
        Long eventId = 10L;

        given(eventService.registerUserForEvent(anyString(), eq(eventId)))
                .willThrow(new AlreadyRegisteredException());

        mockMvc.perform(post("/events/{eventId}/registrations", eventId)
                .with(authenticatedUser()))
                .andExpect(status().isConflict());

        then(eventService).should().registerUserForEvent("user@example.com", eventId);
    }

    @Test
    @DisplayName("POST /events/{eventId}/registrations - should return 404 Not Found when event does not exist")
    void registerForEvent_should_returnNotFound_when_eventDoesNotExist() throws Exception {
        Long eventId = 99L;

        given(eventService.registerUserForEvent(anyString(), eq(eventId)))
                .willThrow(new ResourceNotFoundException("Event", eventId));

        mockMvc.perform(post("/events/{eventId}/registrations", eventId)
                .with(authenticatedUser()))
                .andExpect(status().isNotFound());

        then(eventService).should().registerUserForEvent("user@example.com", eventId);
    }

    @Test
    @DisplayName("POST /events/{eventId}/registrations - should return 401 Unauthorized when request is unauthenticated")
    void registerForEvent_should_returnUnauthorized_when_noAuthentication() throws Exception {
        Long eventId = 10L;

        mockMvc.perform(post("/events/{eventId}/registrations", eventId))
                .andExpect(status().isUnauthorized());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/registrations - should return 204 No Content when authenticated user unregisters")
    void unregisterFromEvent_should_returnNoContent_when_validRequestAsUser() throws Exception {
        Long eventId = 10L;

        mockMvc.perform(delete("/events/{eventId}/registrations", eventId)
                        .with(authenticatedUser()))
                .andExpect(status().isNoContent());

        then(eventService).should().unregisterUserFromEvent(eventId, "user@example.com");
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/registrations - should return 404 Not Found when event does not exist")
    void unregisterFromEvent_should_returnNotFound_when_eventDoesNotExist() throws Exception {
        Long eventId = 99L;

        willThrow(new ResourceNotFoundException("Event", eventId))
                .given(eventService).unregisterUserFromEvent(eventId, "user@example.com");

        mockMvc.perform(delete("/events/{eventId}/registrations", eventId)
                        .with(authenticatedUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/registrations - should return 404 Not Found when user has no active registration")
    void unregisterFromEvent_should_returnNotFound_when_noActiveRegistration() throws Exception {
        Long eventId = 10L;

        willThrow(new ResourceNotFoundException("Registration", eventId))
                .given(eventService).unregisterUserFromEvent(eventId, "user@example.com");

        mockMvc.perform(delete("/events/{eventId}/registrations", eventId)
                        .with(authenticatedUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/registrations - should return 401 Unauthorized when request is unauthenticated")
    void unregisterFromEvent_should_returnUnauthorized_when_noAuthentication() throws Exception {
        mockMvc.perform(delete("/events/{eventId}/registrations", 10L))
                .andExpect(status().isUnauthorized());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /events - should use default pagination and DATE_ASC sort when no params are sent")
    void getAllEvents_should_use_default_pagination_and_sort() throws Exception {
        Pageable defaultPageable = PageRequest.of(0, 20, EventSort.DATE_ASC.toSort());
        EventFilter noFilter = new EventFilter(null, null, null);
        given(eventService.findAllEvents(defaultPageable, noFilter)).willReturn(emptyPage());

        mockMvc.perform(get("/events").with(authenticatedUser()))
                .andExpect(status().isOk());

        then(eventService).should().findAllEvents(defaultPageable, noFilter);
    }

    @Test
    @DisplayName("GET /events - should forward page, size, sort and filter query params to the service")
    void getAllEvents_should_forward_query_params() throws Exception {
        Pageable expectedPageable = PageRequest.of(2, 10, EventSort.NAME_DESC.toSort());
        EventFilter expectedFilter = new EventFilter("rock", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31));
        given(eventService.findAllEvents(expectedPageable, expectedFilter)).willReturn(emptyPage());

        mockMvc.perform(get("/events")
                        .param("page", "2").param("size", "10").param("sort", "NAME_DESC")
                        .param("name", "rock").param("from", "2026-11-01").param("to", "2026-12-31")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        then(eventService).should().findAllEvents(expectedPageable, expectedFilter);
    }

    @Test
    @DisplayName("GET /events - should return 400 Bad Request when sort is not a valid EventSort value")
    void getAllEvents_should_returnBadRequest_when_sortIsInvalid() throws Exception {
        mockMvc.perform(get("/events").param("sort", "foo").with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter sort"))
                .andExpect(jsonPath("$.path").value("/events"));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /events - should return 400 Bad Request without leaking internals when from is not a date")
    void getAllEvents_should_returnBadRequest_when_fromIsNotADate() throws Exception {
        mockMvc.perform(get("/events").param("from", "abc").with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("from: invalid value"))
                .andExpect(jsonPath("$.path").value("/events"));

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /events/{eventId}/registrations - should return 200 and use default pagination and NEWEST sort when ADMIN sends no params")
    void getEventAttendees_should_returnOk_when_adminWithDefaultParams() throws Exception {
        Long eventId = 10L;
        Pageable defaultPageable = PageRequest.of(0, 20, AttendeeSort.NEWEST.toSort());
        given(eventService.findEventAttendees(eventId, defaultPageable)).willReturn(emptyPage());

        mockMvc.perform(get("/events/{eventId}/registrations", eventId))
                .andExpect(status().isOk());

        then(eventService).should().findEventAttendees(eventId, defaultPageable);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /events/{eventId}/registrations - should return 403 Forbidden when user has insufficient role")
    void getEventAttendees_should_returnForbidden_when_roleIsUser() throws Exception {
        mockMvc.perform(get("/events/{eventId}/registrations", 10L))
                .andExpect(status().isForbidden());

        then(eventService).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /events/{eventId}/registrations - should return 404 Not Found when event does not exist")
    void getEventAttendees_should_returnNotFound_when_eventDoesNotExist() throws Exception {
        Long eventId = 99L;
        Pageable defaultPageable = PageRequest.of(0, 20, AttendeeSort.NEWEST.toSort());
        given(eventService.findEventAttendees(eventId, defaultPageable))
                .willThrow(new ResourceNotFoundException("Event", eventId));

        mockMvc.perform(get("/events/{eventId}/registrations", eventId))
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
