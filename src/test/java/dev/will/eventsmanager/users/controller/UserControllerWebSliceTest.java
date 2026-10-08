package dev.will.eventsmanager.users.controller;

import dev.will.eventsmanager.auth.config.RestAuthenticationEntryPoint;
import dev.will.eventsmanager.auth.config.SecurityConfig;
import dev.will.eventsmanager.auth.service.TokenService;
import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.pagination.RegistrationSort;
import dev.will.eventsmanager.users.dto.response.EventSummaryResponse;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import dev.will.eventsmanager.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
    @DisplayName("GET /users/me/registrations - should return 401 when no token is provided")
    void getUserRegisteredEvents_should_return_unauthorized_when_no_token() throws Exception {
        mockMvc.perform(get("/users/me/registrations"))
                .andExpect(status().isUnauthorized());

        then(userService).shouldHaveNoInteractions();
    }



    private static RequestPostProcessor authenticatedUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                "user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private PageResponse<MyRegistrationResponse> emptyPage() {
        return PageResponse.<MyRegistrationResponse>builder().content(List.of()).build();
    }
}
