package dev.will.eventsmanager.dto.response;

public record LoginResponse(
        String token,
        Long userId,
        String email,
        String name
) {

}
