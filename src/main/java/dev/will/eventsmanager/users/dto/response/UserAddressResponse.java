package dev.will.eventsmanager.users.dto.response;

public record UserAddressResponse(
        Long id,
        String street,
        String complement,
        String city,
        String stateOrProvince,
        String postalCode,
        String countryCode
) {}
