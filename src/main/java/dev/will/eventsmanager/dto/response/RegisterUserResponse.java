package dev.will.eventsmanager.dto.response;

public record RegisterUserResponse(
        String name,
        String email,
        Long userId
) {
}

