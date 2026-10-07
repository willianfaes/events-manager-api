package dev.will.eventsmanager.users.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MyRegistrationResponse(
        Long registrationId,
        LocalDateTime registeredAt,
        EventSummaryResponse event
) {
}
