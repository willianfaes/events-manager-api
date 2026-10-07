package dev.will.eventsmanager.users.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventSummaryResponse(
        Long id,
        String title,
        LocalDateTime date,
        String location
) {
}
