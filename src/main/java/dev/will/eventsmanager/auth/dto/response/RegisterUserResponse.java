package dev.will.eventsmanager.auth.dto.response;

public record RegisterUserResponse(
        String name,
        String email,
        Long userId
) {
}

