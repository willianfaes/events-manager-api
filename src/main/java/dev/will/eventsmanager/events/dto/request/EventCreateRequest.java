package dev.will.eventsmanager.events.dto.request;

import dev.will.eventsmanager.events.entity.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventCreateRequest(
    @NotBlank(message = "Name cannot be empty")
    String name,

    @NotNull(message = "Category cannot be null")
    Category category,

    @NotNull(message = "Schedule cannot be null")
    LocalDateTime schedule,

    @NotNull(message = "Address cannot be null")
    @Valid
    EventAddressRequest address,

    String description
) {
}
