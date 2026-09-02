package dev.will.eventsmanager.events.dto.response;

public record EventAddressResponse(
        Long id,
        String street,
        String complement,
        String city,
        String stateOrProvince,
        String postalCode,
        String countryCode,
        String venueName
) {}
