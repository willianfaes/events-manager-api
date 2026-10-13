package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.auth.config.RestAuthenticationEntryPoint;
import dev.will.eventsmanager.auth.config.SecurityConfig;
import dev.will.eventsmanager.auth.service.TokenService;
import dev.will.eventsmanager.common.exception.AlreadyRegisteredException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.common.pagination.EventSort;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.emptyPage;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @DisplayName("POST /events/{eventId}/registrations - should return 201 Created and registration id when authenticated user registers for existing event")
    void registerForEvent_should_returnCreated_when_validRequestAsUser() throws Exception {
        Long eventId = 10L;
        Long registrationId = 1L;
        EventRegistrationResponse expectedResponse = new EventRegistrationResponse(registrationId, eventId);

        given(eventService.registerUserForEvent(anyString(), eq(eventId))).willReturn(expectedResponse);

        mockMvc.perform(post("/events/{eventId}/registrations", eventId)
                .with(authenticatedUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(registrationId))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(header().string("Location", endsWith("/events/" + eventId + "/registrations/me")));


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
    @DisplayName("GET /events/{eventId}/registrations/me - should return 200 with the user's registration")
    void getMyRegistration_should_returnOk_when_userIsRegistered() throws Exception {
        Long eventId = 10L;

        given(eventService.findUserRegistration(eventId, "user@example.com"))
                .willReturn(new EventRegistrationResponse(1L, eventId));

        mockMvc.perform(get("/events/{eventId}/registrations/me", eventId)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventId").value(eventId));
    }

    @Test
    @DisplayName("GET /events/{eventId}/registrations/me - should return 404 Not Found when the user has no active registration")
    void getMyRegistration_should_returnNotFound_when_noActiveRegistration() throws Exception {
        Long eventId = 10L;

        given(eventService.findUserRegistration(eventId, "user@example.com"))
                .willThrow(new ResourceNotFoundException("Registration", eventId));

        mockMvc.perform(get("/events/{eventId}/registrations/me", eventId)
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
}
