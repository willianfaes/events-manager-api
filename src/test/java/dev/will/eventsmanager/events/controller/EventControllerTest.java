package dev.will.eventsmanager.events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.will.eventsmanager.auth.config.RestAuthenticationEntryPoint;
import dev.will.eventsmanager.auth.config.SecurityConfig;
import dev.will.eventsmanager.auth.service.TokenService;
import dev.will.eventsmanager.common.exception.AlreadyRegisteredException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TokenService tokenService;

    private static RequestPostProcessor authenticatedUser(String email) {
        return authentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private EventCreateRequest.EventCreateRequestBuilder validRequest() {
        return EventCreateRequest.builder()
                .name("Summit 2027")
                .category(Category.PARTY)
                .schedule(LocalDateTime.now().plusDays(10))
                .address(validAddress())
                .description("Valid description");
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

    private EventResponse createValidResponse(Long id, EventCreateRequest request) {
        EventAddressResponse addressResponse = new EventAddressResponse(
                id,
                request.address().street(),
                "Rooftop",
                request.address().city(),
                "SP",
                request.address().postalCode(),
                request.address().countryCode(),
                "Skyline Lounge"
        );

        return new EventResponse(
                id,
                request.name(),
                request.category(),
                request.schedule(),
                addressResponse,
                request.description(),
                null
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 201 Created with Location header when ADMIN provides valid request")
    void createEvent_should_returnCreated_when_validRequestAsAdmin() throws Exception {
        Long generatedId = 88L;
        EventCreateRequest request = validRequest().build();
        EventResponse expectedResponse = createValidResponse(generatedId, request);

        given(eventService.createEvent(any(EventCreateRequest.class))).willReturn(expectedResponse);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/events/" + generatedId))
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.id").value(generatedId))
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.category").value(request.category().name()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.address.street").value(request.address().street()));

        ArgumentCaptor<EventCreateRequest> captor = ArgumentCaptor.forClass(EventCreateRequest.class);
        verify(eventService).createEvent(captor.capture());
        EventCreateRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.name()).isEqualTo(request.name());
        assertThat(capturedRequest.category()).isEqualTo(request.category());
        assertThat(capturedRequest.description()).isEqualTo(request.description());
        assertThat(capturedRequest.address().street()).isEqualTo(request.address().street());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 201 Created when description has exactly 1000 characters")
    void createEvent_should_returnCreated_when_descriptionIs1000Chars() throws Exception {
        Long generatedId = 105L;
        String description1000 = "A".repeat(1000);

        EventAddressRequest addressRequest = validAddress();
        EventCreateRequest request = validRequest().description(description1000).address(addressRequest).build();
        EventResponse expectedResponse = createValidResponse(generatedId, request);

        given(eventService.createEvent(any(EventCreateRequest.class))).willReturn(expectedResponse);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/events/" + generatedId));

        ArgumentCaptor<EventCreateRequest> captor = ArgumentCaptor.forClass(EventCreateRequest.class);
        verify(eventService).createEvent(captor.capture());
        assertThat(captor.getValue().description()).hasSize(1000);
    }

    @Test
    @DisplayName("createEvent should return 401 Unauthorized when request is unauthenticated")
    void createEvent_should_returnUnauthorized_when_noAuthentication() throws Exception {
        EventCreateRequest request = validRequest().build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/events"));

        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("createEvent should return 403 Forbidden when user has insufficient role")
    void createEvent_should_returnForbidden_when_roleIsUser() throws Exception {
        EventCreateRequest request = validRequest().build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/events"));

        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 400 Bad Request when category is missing")
    void createEvent_should_returnBadRequest_when_categoryIsNull() throws Exception {
        EventAddressRequest addressRequest = validAddress("Main St");
        EventCreateRequest request = validRequest().category(null).address(addressRequest).build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("category")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/events"));

        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 400 Bad Request when schedule is in the past")
    void createEvent_should_returnBadRequest_when_scheduleIsInPast() throws Exception {
        EventAddressRequest addressRequest = validAddress();
        EventCreateRequest request = validRequest().schedule(LocalDateTime.now().minusDays(1)).address(addressRequest).build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("schedule")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/events"));

        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createEvent should return 400 Bad Request when description exceeds 1000 characters")
    void createEvent_should_returnBadRequest_when_descriptionExceedsLimit() throws Exception {
        String description1001 = "A".repeat(1001);
        EventAddressRequest addressRequest = validAddress();
        EventCreateRequest request = validRequest().description(description1001).address(addressRequest).build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("description")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/events"));

        verifyNoInteractions(eventService);
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
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("street")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/events"));

        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("registerForEvent should return 201 Created and registration id when authenticated user registers for existing event")
    void registerForEvent_should_returnCreated_when_validRequestAsUser() throws Exception {
        Long eventId = 10L;
        Long registrationId = 1L;
        EventRegistrationResponse expectedResponse = new EventRegistrationResponse(registrationId);

        given(eventService.registerUserForEvent("user@example.com", eventId)).willReturn(expectedResponse);

        mockMvc.perform(post("/events/{eventId}/registrations", eventId)
                .with(authenticatedUser("user@example.com")));

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> eventIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(eventService).registerUserForEvent(emailCaptor.capture(), eventIdCaptor.capture());
        assertThat(emailCaptor.getValue()).isEqualTo("user@example.com");
        assertThat(eventIdCaptor.getValue()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("registerForEvent should return 409 Conflict when user is already registered for event")
    void registerForEvent_should_returnConflict_when_alreadyRegistered() throws Exception {
        Long eventId = 10L;

        given(eventService.registerUserForEvent("user@example.com", eventId))
                .willThrow(new AlreadyRegisteredException());

        mockMvc.perform(post("/events/{eventId}/registrations", eventId)
                .with(authenticatedUser("user@example.com")));

        verify(eventService).registerUserForEvent("user@example.com", eventId);
    }

    @Test
    @DisplayName("registerForEvent should return 404 Not Found when event does not exist")
    void registerForEvent_should_returnNotFound_when_eventDoesNotExist() throws Exception {
        Long eventId = 99L;

        given(eventService.registerUserForEvent("user@example.com", eventId))
                .willThrow(new ResourceNotFoundException("Event", eventId));

        mockMvc.perform(post("/events/{eventId}/registrations", eventId)
                .with(authenticatedUser("user@example.com")));

        verify(eventService).registerUserForEvent("user@example.com", eventId);
    }

    @Test
    @DisplayName("registerForEvent should return 401 Unauthorized when request is unauthenticated")
    void registerForEvent_should_returnUnauthorized_when_noAuthentication() throws Exception {
        Long eventId = 10L;

        mockMvc.perform(post("/events/{eventId}/registrations", eventId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/events/" + eventId + "/registrations"));

        verifyNoInteractions(eventService);
    }
}
