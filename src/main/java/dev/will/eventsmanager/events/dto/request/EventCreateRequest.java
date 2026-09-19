package dev.will.eventsmanager.events.dto.request;

import dev.will.eventsmanager.events.entity.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventCreateRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        Category category,

        @NotNull
        @Future
        LocalDateTime schedule,

        @NotNull
        @Valid
        EventAddressRequest address,

        @Size(max = 1000)
        String description
) {
}
