package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.common.geocoding.Geocoder;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.repository.EventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@WithMockUser(roles = "ADMIN")
class EventGeocodingIntegrationTest extends AbstractIntegrationTest {

    private static final Coordinates VERONA = new Coordinates(45.4390, 10.9942);
    private static final Coordinates ROMA = new Coordinates(41.9028, 12.4964);
    private static final String BODY = """
            {
              "name": "Geocoded Event",
              "category": "CONCERT",
              "schedule": "2126-10-01T20:00:00",
              "description": "Any",
              "capacity": 10,
              "address": {
                "street": "Piazza Bra 1",
                "city": "Verona",
                "postalCode": "37121",
                "countryCode": "IT"
              }
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EntityManager entityManager;

    @MockitoBean
    Geocoder geocoder;

    @Test
    @DisplayName("POST /admin/events - should save and return the coordinates of the address")
    void createEvent_whenTheAddressIsFound_savesTheCoordinates() throws Exception {
        given(geocoder.locate(anyString())).willReturn(Optional.of(VERONA));

        mockMvc.perform(post("/admin/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address.latitude").value(VERONA.latitude()))
                .andExpect(jsonPath("$.address.longitude").value(VERONA.longitude()));
    }

    @Test
    @DisplayName("POST /admin/events - should still create the event when there are no coordinates")
    void createEvent_whenThereAreNoCoordinates_createsTheEvent() throws Exception {
        given(geocoder.locate(anyString())).willReturn(Optional.empty());

        mockMvc.perform(post("/admin/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address.latitude").value(nullValue()))
                .andExpect(jsonPath("$.address.longitude").value(nullValue()));
    }

    @Test
    @DisplayName("PUT /admin/events/{eventId} - should replace the coordinates")
    void updateEvent_whenTheAddressIsFound_replacesTheCoordinates() throws Exception {
        Long eventId = createEventInVerona();
        given(geocoder.locate(anyString())).willReturn(Optional.of(ROMA));

        mockMvc.perform(put("/admin/events/{eventId}", eventId).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        then(reloadedEvent(eventId).getAddress().getLatitude()).isEqualTo(ROMA.latitude());
    }

    @Test
    @DisplayName("PUT /admin/events/{eventId} - should clear the coordinates when the new address has none")
    void updateEvent_whenThereAreNoCoordinates_clearsThem() throws Exception {
        Long eventId = createEventInVerona();
        given(geocoder.locate(anyString())).willReturn(Optional.empty());

        mockMvc.perform(put("/admin/events/{eventId}", eventId).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        then(reloadedEvent(eventId).getAddress().getLatitude()).isNull();
    }

    private Long createEventInVerona() throws Exception {
        given(geocoder.locate(anyString())).willReturn(Optional.of(VERONA));
        String location = mockMvc.perform(post("/admin/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andReturn().getResponse().getHeader("Location");
        return Long.valueOf(requireNonNull(location).substring(location.lastIndexOf('/') + 1));
    }

    private Event reloadedEvent(Long eventId) {
        entityManager.flush();
        entityManager.clear();
        return eventRepository.findByIdWithAddress(eventId).orElseThrow();
    }
}
