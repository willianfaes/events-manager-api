package dev.will.eventsmanager.auth.dto.response;

public record LoginResponse(
        String token,
        Long userId,
        String email,
        String name
) {

}
