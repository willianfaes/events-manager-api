package dev.will.eventsmanager.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "demo@example.com")
        @NotBlank
        @Email(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Invalid credentials"
        )
        String email,
        @Schema(example = "Demo1234!")
        String password) {
}
