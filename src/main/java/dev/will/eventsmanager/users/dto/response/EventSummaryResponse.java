package dev.will.eventsmanager.users.dto.response;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long id,
        String title,
        LocalDateTime date,
        String location
) {
}
