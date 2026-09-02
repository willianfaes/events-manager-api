package dev.will.eventsmanager.events.dto.request;

import dev.will.eventsmanager.events.entity.Category;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank(message = "Event name cannot be empty")
        String name,

        @NotNull(message = "Event category cannot be null")
        Category category,

        @NotNull(message = "Event schedule cannot be null")
        @FutureOrPresent(message = "Event schedule must be in the present or future")
        LocalDateTime schedule,

        EventAddressRequest address




) {
}

