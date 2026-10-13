package dev.will.eventsmanager.events.dto.response;

import dev.will.eventsmanager.events.entity.Category;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventResponse(

        Long id,
        String name,
        Category category,
        LocalDateTime schedule,
        EventAddressResponse address,
        String description,
        int capacity,
        int remainingSpots
) {
}