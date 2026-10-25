package dev.will.eventsmanager.users.dto.response;

public record ProfileResponse(
        String name,
        Integer age,
        UserAddressResponse address
) {}
