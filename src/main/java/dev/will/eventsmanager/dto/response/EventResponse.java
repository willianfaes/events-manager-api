package dev.will.eventsmanager.dto.response;

import dev.will.eventsmanager.entity.Category;
import dev.will.eventsmanager.entity.EventAddress;
import dev.will.eventsmanager.entity.EventRegistration;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(

        Long id,
        String name,
        Category category,
        LocalDateTime schedule,
        EventAddress address,
        String description,
        List<EventRegistration> registrations
) {
}