package dev.will.eventsmanager.auth.dto.request;

import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterUserRequest(
        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank
        @Email(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Please provide a valid email (e.g. name@domain.com)"
        )
        String email,

        Integer age,

        @NotBlank(message = "Senha é obrigatória")
        String password,

        @NotNull(message = "Address information is required")
        @Valid
        UserAddressRequest address
) {}
