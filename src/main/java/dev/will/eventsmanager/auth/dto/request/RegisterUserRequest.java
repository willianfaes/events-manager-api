package dev.will.eventsmanager.auth.dto.request;

import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterUserRequest(
        @Schema(example = "Demo User")
        @NotBlank(message = "Nome é obrigatório")
        String name,

        @Schema(example = "demo@example.com")
        @NotBlank
        @Email(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Please provide a valid email (e.g. name@domain.com)"
        )
        String email,

        @Schema(example = "30")
        Integer age,

        @Schema(example = "Demo1234!")
        @NotBlank(message = "Senha é obrigatória")
        String password,

        @NotNull(message = "Address information is required")
        @Valid
        UserAddressRequest address
) {}
