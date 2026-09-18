package dev.will.eventsmanager.events.dto.response;

import lombok.Builder;

@Builder
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
