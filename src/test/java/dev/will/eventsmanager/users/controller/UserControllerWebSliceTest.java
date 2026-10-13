package dev.will.eventsmanager.users.controller;

import dev.will.eventsmanager.auth.config.RestAuthenticationEntryPoint;
import dev.will.eventsmanager.auth.config.SecurityConfig;
import dev.will.eventsmanager.auth.service.TokenService;
import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.pagination.RegistrationSort;
import dev.will.eventsmanager.users.dto.response.EventSummaryResponse;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import dev.will.eventsmanager.users.dto.response.RegisteredEventsResponse;
import dev.will.eventsmanager.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.emptyPage;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class UserControllerWebSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @DisplayName("GET /users/me/registrations - should return registered events with default pagination")
    void getUserRegisteredEvents_should_return_registered_events() throws Exception {
        Long registrationId = 1L;
        Long eventId = 2L;

        EventSummaryResponse eventSummaryResponse = EventSummaryResponse.builder()
                .id(eventId)
                .build();

        MyRegistrationResponse myRegistrationResponse = MyRegistrationResponse.builder()
                .registrationId(registrationId)
                .event(eventSummaryResponse)
                .build();

        PageResponse<MyRegistrationResponse> registeredEvents = PageResponse.<MyRegistrationResponse>builder()
                .content(List.of(myRegistrationResponse))
                .build();

        Pageable defaultPageable = PageRequest.of(0, 20, RegistrationSort.NEWEST.toSort());

        given(userService.getUserRegisteredEvents("user@example.com", defaultPageable))
                .willReturn(registeredEvents);

        mockMvc.perform(get("/users/me/registrations").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].registrationId").value(registrationId))
                .andExpect(jsonPath("$.content[0].event.id").value(eventId));

        then(userService).should().getUserRegisteredEvents("user@example.com", defaultPageable);
    }

    @Test
    @DisplayName("GET /users/me/registrations - should forward page, size and sort query params to the service")
    void getUserRegisteredEvents_should_forward_pagination_params() throws Exception {
        Pageable expected = PageRequest.of(3, 10, RegistrationSort.OLDEST.toSort());
        given(userService.getUserRegisteredEvents("user@example.com", expected)).willReturn(emptyPage());

        mockMvc.perform(get("/users/me/registrations")
                        .param("page", "3").param("size", "10").param("sort", "OLDEST")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        then(userService).should().getUserRegisteredEvents("user@example.com", expected);
    }

    @Test
    @DisplayName("GET /users/me/registrations - should clamp page size to the maximum allowed")
    void getUserRegisteredEvents_should_clamp_size_when_above_maximum() throws Exception {
        Pageable expected = PageRequest.of(0, 50, RegistrationSort.NEWEST.toSort());
        given(userService.getUserRegisteredEvents("user@example.com", expected)).willReturn(emptyPage());

        mockMvc.perform(get("/users/me/registrations").param("size", "1000")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        then(userService).should().getUserRegisteredEvents("user@example.com", expected);
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should return 200 with the registered event ids")
    void getRegisteredEventIds_should_return_ids_found_by_the_service() throws Exception {
        given(userService.findRegisteredEventIds("user@example.com", Set.of(1L, 2L, 3L, 5L)))
                .willReturn(new RegisteredEventsResponse(List.of(2L, 5L)));

        mockMvc.perform(get("/users/me/registrations/events").param("eventIds", "1,2,3,5")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventIds[0]").value(2))
                .andExpect(jsonPath("$.eventIds[1]").value(5));
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should pass repeated ids to the service only once")
    void getRegisteredEventIds_should_remove_repeated_ids() throws Exception {
        given(userService.findRegisteredEventIds("user@example.com", Set.of(1L, 2L)))
                .willReturn(new RegisteredEventsResponse(List.of()));

        mockMvc.perform(get("/users/me/registrations/events").param("eventIds", "1,1,2,2")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        then(userService).should().findRegisteredEventIds("user@example.com", Set.of(1L, 2L));
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should return 400 naming the parameter when eventIds is missing")
    void getRegisteredEventIds_should_return_bad_request_when_param_is_missing() throws Exception {
        mockMvc.perform(get("/users/me/registrations/events").with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("eventIds")));

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should return 400 naming the parameter when eventIds is empty")
    void getRegisteredEventIds_should_return_bad_request_when_param_is_empty() throws Exception {
        mockMvc.perform(get("/users/me/registrations/events").param("eventIds", "")
                        .with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("eventIds")));

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should return 400 naming the parameter when an id is not a number")
    void getRegisteredEventIds_should_return_bad_request_when_an_id_is_not_numeric() throws Exception {
        mockMvc.perform(get("/users/me/registrations/events").param("eventIds", "1,abc")
                        .with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("eventIds")));

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should return 400 naming the parameter when there are more than 50 ids")
    void getRegisteredEventIds_should_return_bad_request_when_there_are_more_than_50_ids() throws Exception {
        String fiftyOneIds = IntStream.rangeClosed(1, 51)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        mockMvc.perform(get("/users/me/registrations/events").param("eventIds", fiftyOneIds)
                        .with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("eventIds")));

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /users/me/registrations - should return 401 when no token is provided")
    void getUserRegisteredEvents_should_return_unauthorized_when_no_token() throws Exception {
        mockMvc.perform(get("/users/me/registrations"))
                .andExpect(status().isUnauthorized());

        then(userService).shouldHaveNoInteractions();
    }
}
