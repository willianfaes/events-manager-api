package dev.will.eventsmanager.users.controller;

import dev.will.eventsmanager.auth.config.RestAccessDeniedHandler;
import dev.will.eventsmanager.auth.config.RestAuthenticationEntryPoint;
import dev.will.eventsmanager.auth.config.SecurityConfig;
import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.exception.ProfileAlreadyExistsException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.common.pagination.RegistrationSort;
import dev.will.eventsmanager.users.dto.request.ProfileRequest;
import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import dev.will.eventsmanager.users.dto.response.EventSummaryResponse;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import dev.will.eventsmanager.users.dto.response.RegisteredEventsResponse;
import dev.will.eventsmanager.users.dto.response.UserAddressResponse;
import dev.will.eventsmanager.users.service.UserGeocodingCoordinator;
import dev.will.eventsmanager.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.will.eventsmanager.TestFixtures.USER_SUB;
import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.emptyPage;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Who may call these routes (401 without a token) is tested once, for every route, in SecurityAccessRulesWebSliceTest;
// the tests here assume a signed-in user and cover what each route does.
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class UserControllerWebSliceTest {

    private static final String PROFILE_JSON = """
            {"name": "Demo User", "age": 30, "address": {"street": "Via del Corso 1", "city": "Rome",
             "postalCode": "00186", "countryCode": "IT"}}
            """;
    private static final ProfileRequest PROFILE_REQUEST = new ProfileRequest("Demo User", 30,
            new UserAddressRequest("Via del Corso 1", null, "Rome", null, "00186", "IT"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserGeocodingCoordinator userGeocodingCoordinator;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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

        given(userService.getUserRegisteredEvents(USER_SUB, defaultPageable))
                .willReturn(registeredEvents);

        mockMvc.perform(get("/users/me/registrations").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].registrationId").value(registrationId))
                .andExpect(jsonPath("$.content[0].event.id").value(eventId));

        then(userService).should().getUserRegisteredEvents(USER_SUB, defaultPageable);
    }

    @Test
    @DisplayName("GET /users/me/registrations - should forward page, size and sort query params to the service")
    void getUserRegisteredEvents_should_forward_pagination_params() throws Exception {
        Pageable expected = PageRequest.of(3, 10, RegistrationSort.OLDEST.toSort());
        given(userService.getUserRegisteredEvents(USER_SUB, expected)).willReturn(emptyPage());

        mockMvc.perform(get("/users/me/registrations")
                        .param("page", "3").param("size", "10").param("sort", "OLDEST")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        then(userService).should().getUserRegisteredEvents(USER_SUB, expected);
    }

    @Test
    @DisplayName("GET /users/me/registrations - should clamp page size to the maximum allowed")
    void getUserRegisteredEvents_should_clamp_size_when_above_maximum() throws Exception {
        Pageable expected = PageRequest.of(0, 50, RegistrationSort.NEWEST.toSort());
        given(userService.getUserRegisteredEvents(USER_SUB, expected)).willReturn(emptyPage());

        mockMvc.perform(get("/users/me/registrations").param("size", "1000")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        then(userService).should().getUserRegisteredEvents(USER_SUB, expected);
    }

    @Test
    @DisplayName("GET /users/me/registrations/events - should return 200 with the registered event ids")
    void getRegisteredEventIds_should_return_ids_found_by_the_service() throws Exception {
        given(userService.findRegisteredEventIds(USER_SUB, Set.of(1L, 2L, 3L, 5L)))
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
        given(userService.findRegisteredEventIds(USER_SUB, Set.of(1L, 2L)))
                .willReturn(new RegisteredEventsResponse(List.of()));

        mockMvc.perform(get("/users/me/registrations/events").param("eventIds", "1,1,2,2")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        then(userService).should().findRegisteredEventIds(USER_SUB, Set.of(1L, 2L));
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
    @DisplayName("POST /users/me - should return 201 with the profile and a Location pointing to GET /users/me")
    void createProfile_should_return_created_profile() throws Exception {
        given(userGeocodingCoordinator.createProfile(USER_SUB, PROFILE_REQUEST)).willReturn(new ProfileResponse("Demo User", 30,
                new UserAddressResponse(1L, "Via del Corso 1", null, "Rome", null, "00186", "IT")));

        mockMvc.perform(post("/users/me").contentType(MediaType.APPLICATION_JSON).content(PROFILE_JSON)
                        .with(authenticatedUser()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/users/me")))
                .andExpect(jsonPath("$.name").value("Demo User"))
                .andExpect(jsonPath("$.address.city").value("Rome"));
    }

    @Test
    @DisplayName("POST /users/me - should return 400 when the name is missing")
    void createProfile_should_return_bad_request_when_name_is_missing() throws Exception {
        mockMvc.perform(post("/users/me").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"age": 30, "address": {"street": "Via del Corso 1", "city": "Rome",
                                 "postalCode": "00186", "countryCode": "IT"}}
                                """)
                        .with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));

        then(userGeocodingCoordinator).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /users/me - should return 409 when the profile already exists")
    void createProfile_should_return_conflict_when_profile_exists() throws Exception {
        given(userGeocodingCoordinator.createProfile(USER_SUB, PROFILE_REQUEST)).willThrow(new ProfileAlreadyExistsException());

        mockMvc.perform(post("/users/me").contentType(MediaType.APPLICATION_JSON).content(PROFILE_JSON)
                        .with(authenticatedUser()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /users/me - should return 200 with the profile of the token's sub")
    void getProfile_should_return_the_profile() throws Exception {
        given(userService.getProfile(USER_SUB)).willReturn(new ProfileResponse("Demo User", 30,
                new UserAddressResponse(1L, "Via del Corso 1", null, "Rome", null, "00186", "IT")));

        mockMvc.perform(get("/users/me").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Demo User"))
                .andExpect(jsonPath("$.address.city").value("Rome"));
    }

    @Test
    @DisplayName("GET /users/me - should return 404 when the sub has no profile yet")
    void getProfile_should_return_not_found_without_profile() throws Exception {
        given(userService.getProfile(USER_SUB)).willThrow(new ResourceNotFoundException("Profile", USER_SUB));

        mockMvc.perform(get("/users/me").with(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /users/me - should return 200 with the updated profile")
    void updateProfile_should_return_the_updated_profile() throws Exception {
        given(userGeocodingCoordinator.updateProfile(USER_SUB, PROFILE_REQUEST)).willReturn(new ProfileResponse("Demo User", 30,
                new UserAddressResponse(1L, "Via del Corso 1", null, "Rome", null, "00186", "IT")));

        mockMvc.perform(put("/users/me").contentType(MediaType.APPLICATION_JSON).content(PROFILE_JSON)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.name").value("Demo User"))
                .andExpect(jsonPath("$.address.city").value("Rome"));
    }

    @Test
    @DisplayName("PUT /users/me - should return 400 when the address is missing")
    void updateProfile_should_return_bad_request_when_address_is_missing() throws Exception {
        mockMvc.perform(put("/users/me").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Demo User", "age": 30}
                                """)
                        .with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("address")));

        then(userGeocodingCoordinator).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PUT /users/me - should return 404 when the sub has no profile yet")
    void updateProfile_should_return_not_found_without_profile() throws Exception {
        given(userGeocodingCoordinator.updateProfile(USER_SUB, PROFILE_REQUEST))
                .willThrow(new ResourceNotFoundException("Profile", USER_SUB));

        mockMvc.perform(put("/users/me").contentType(MediaType.APPLICATION_JSON).content(PROFILE_JSON)
                        .with(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
