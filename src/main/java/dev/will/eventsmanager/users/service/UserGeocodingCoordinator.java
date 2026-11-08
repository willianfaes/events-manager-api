package dev.will.eventsmanager.users.service;

import dev.will.eventsmanager.common.exception.DemoProfileReadOnlyException;
import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.common.geocoding.Geocoder;
import dev.will.eventsmanager.users.dto.request.ProfileRequest;
import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserGeocodingCoordinator {

    private final Geocoder geocoder;
    private final UserService userService;
    private final String demoUserSub;

    public UserGeocodingCoordinator(Geocoder geocoder, UserService userService,
                                    @Value("${app.demo.user-sub:}") String demoUserSub) {
        this.geocoder = geocoder;
        this.userService = userService;
        this.demoUserSub = demoUserSub;
    }

    public ProfileResponse createProfile(String externalId, ProfileRequest request) {
        return userService.createProfile(externalId, request, locate(request.address()));
    }

    // The demo password is public, so anyone could rewrite the shared profile. Checked before the lookup, so a
    // refused request never reaches the geocoding provider. With no demo user configured the property is empty,
    // which matches no sub.
    public ProfileResponse updateProfile(String externalId, ProfileRequest request) {
        if (demoUserSub.equals(externalId)) {
            throw new DemoProfileReadOnlyException();
        }
        return userService.updateProfile(externalId, request, locate(request.address()));
    }

    // No street: the distance only needs the area, and the home address never leaves the API.
    private Coordinates locate(UserAddressRequest address) {
        String area = "%s %s, %s".formatted(address.postalCode(), address.city(), address.countryCode());
        return geocoder.locate(area).orElse(null);
    }
}
