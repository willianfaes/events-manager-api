package dev.will.eventsmanager.events.dto.response;

import dev.will.eventsmanager.events.entity.Category;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(

        Long id,
        String name,
        Category category,
        LocalDateTime schedule,
        EventAddressResponse address,
        String description,
        List<EventRegistration> registrations
) {
}