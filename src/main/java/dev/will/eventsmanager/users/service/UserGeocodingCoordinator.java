package dev.will.eventsmanager.users.service;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.common.geocoding.Geocoder;
import dev.will.eventsmanager.users.dto.request.CreateProfileRequest;
import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserGeocodingCoordinator {

    private final Geocoder geocoder;
    private final UserService userService;

    public ProfileResponse createProfile(String externalId, CreateProfileRequest request) {
        return userService.createProfile(externalId, request, locate(request.address()));
    }

    // No street: the distance only needs the area, and the home address never leaves the API.
    private Coordinates locate(UserAddressRequest address) {
        String area = "%s %s, %s".formatted(address.postalCode(), address.city(), address.countryCode());
        return geocoder.locate(area).orElse(null);
    }
}
