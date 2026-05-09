package dev.will.eventsmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.mapstruct.Mapping;

public record AddressRequest(
        @NotBlank(message = "Street cannot be empty")
        @Size(max = 255, message = "Street must not exceed 255 characters")
        String street,

        @NotBlank(message = "Number cannot be empty")
        @Size(max = 20, message = "Number must not exceed 20 characters")
        String number,

        @Size(max = 100, message = "Complement must not exceed 100 characters")
        String complement,

        @NotBlank(message = "Neighborhood cannot be empty")
        @Size(max = 100, message = "Neighborhood must not exceed 100 characters")
        String neighborhood,

        @NotBlank(message = "City cannot be empty")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "State cannot be empty")
        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @Size(max = 20, message = "Zip code must not exceed 20 characters")
        String postal_code,

        @NotBlank(message = "Country cannot be empty")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country
) {}