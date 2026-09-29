package dev.will.eventsmanager.events.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record EventAddressRequest(
        @NotBlank(message = "Street cannot be empty")
        @Size(max = 150, message = "Street must not exceed 150 characters")
        String street,

        @Size(max = 100, message = "Complement must not exceed 100 characters")
        String complement,

        @NotBlank(message = "City cannot be empty")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "State/province must not exceed 100 characters")
        String stateOrProvince,

        @NotBlank(message = "Postal code cannot be empty")
        @Size(max = 12, message = "Postal code must not exceed 12 characters")
        String postalCode,

        @NotBlank(message = "Country code cannot be empty")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be a valid ISO 3166-1 alpha-2 code")
        String countryCode,

        @Size(max = 255, message = "Venue name must not exceed 255 characters")
        String venueName
) {}

