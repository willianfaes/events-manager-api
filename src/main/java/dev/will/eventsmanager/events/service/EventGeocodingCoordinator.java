package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.common.geocoding.Geocoder;
import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EventGeocodingCoordinator {

    private final Geocoder geocoder;
    private final EventService eventService;

    public EventResponse createEvent(EventCreateRequest request) {
        return eventService.createEvent(request, locate(request.address()));
    }

    public EventResponse updateEvent(Long eventId, EventCreateRequest request) {
        return eventService.updateEvent(eventId, request, locate(request.address()));
    }

    private Coordinates locate(EventAddressRequest address) {
        String text = "%s, %s %s, %s".formatted(
                address.street(), address.postalCode(), address.city(), address.countryCode());
        return geocoder.locate(text).orElse(null);
    }
}
