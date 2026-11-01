package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.common.geocoding.Geocoder;
import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EventGeocodingCoordinatorTest {

    private static final String ADDRESS_TEXT = "Piazza Bra 1, 37121 Verona, IT";
    private static final Coordinates COORDINATES = new Coordinates(45.4390, 10.9942);
    private static final EventCreateRequest REQUEST = EventCreateRequest.builder()
            .address(EventAddressRequest.builder()
                    .street("Piazza Bra 1")
                    .complement("Gate B")
                    .postalCode("37121")
                    .city("Verona")
                    .countryCode("IT")
                    .build())
            .build();

    @Mock
    Geocoder geocoder;

    @Mock
    EventService eventService;

    @InjectMocks
    EventGeocodingCoordinator coordinator;

    @Test
    void createEvent_passesTheCoordinatesToTheService() {
        EventResponse expected = EventResponse.builder().id(1L).build();
        given(geocoder.locate(ADDRESS_TEXT)).willReturn(Optional.of(COORDINATES));
        given(eventService.createEvent(REQUEST, COORDINATES)).willReturn(expected);

        then(coordinator.createEvent(REQUEST)).isSameAs(expected);
    }

    @Test
    void createEvent_whenThereAreNoCoordinates_stillCreatesTheEvent() {
        EventResponse expected = EventResponse.builder().id(1L).build();
        given(geocoder.locate(ADDRESS_TEXT)).willReturn(Optional.empty());
        given(eventService.createEvent(REQUEST, null)).willReturn(expected);

        then(coordinator.createEvent(REQUEST)).isSameAs(expected);
    }

    @Test
    void updateEvent_passesTheCoordinatesToTheService() {
        EventResponse expected = EventResponse.builder().id(7L).build();
        given(geocoder.locate(ADDRESS_TEXT)).willReturn(Optional.of(COORDINATES));
        given(eventService.updateEvent(7L, REQUEST, COORDINATES)).willReturn(expected);

        then(coordinator.updateEvent(7L, REQUEST)).isSameAs(expected);
    }
}
