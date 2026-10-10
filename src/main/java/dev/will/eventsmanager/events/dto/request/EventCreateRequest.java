package dev.will.eventsmanager.events.dto.request;

import dev.will.eventsmanager.events.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventCreateRequest(
        @Schema(description = "Event name", example = "Rock in Rome")
        @NotBlank
        @Size(max = 255)
        String name,

        @Schema(description = "Event category", example = "CONCERT")
        @NotNull
        Category category,

        @Schema(description = "Date and time of the event. Must be in the future", example = "2030-01-01T20:00:00")
        @NotNull
        @Future
        LocalDateTime schedule,

        @NotNull
        @Valid
        EventAddressRequest address,

        @Schema(description = "Optional event description", example = "An evening of live rock music.")
        @Size(max = 1000)
        String description,

        @Schema(description = "Maximum number of active registrations. Must be positive", example = "50")
        @NotNull
        @Positive
        Integer capacity
) {
}
