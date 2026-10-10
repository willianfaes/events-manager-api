package dev.will.eventsmanager.events.dto.response;

import java.time.LocalDateTime;

public record EventAttendeeResponse(
        Long registrationId,
        Long userId,
        String name,
        String email,
        LocalDateTime registeredAt
) {
}
