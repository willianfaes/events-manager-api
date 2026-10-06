package dev.will.eventsmanager.users.dto.response;

import java.time.LocalDateTime;

public record MyRegistrationResponse(
        Long registrationId,
        LocalDateTime registeredAt,
        EventSummaryResponse event
) {
}
