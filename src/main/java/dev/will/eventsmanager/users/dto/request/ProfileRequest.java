package dev.will.eventsmanager.users.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileRequest(
        @Schema(example = "Demo User")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(example = "30")
        Integer age,

        @NotNull(message = "Address information is required")
        @Valid
        UserAddressRequest address
) {}
