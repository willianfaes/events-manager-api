package dev.will.eventsmanager.users.service;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.common.geocoding.Geocoder;
import dev.will.eventsmanager.users.dto.request.ProfileRequest;
import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static dev.will.eventsmanager.TestFixtures.USER_SUB;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserGeocodingCoordinatorTest {

    // The street is left out on purpose: the home address must not reach the provider.
    private static final String AREA = "20121 Milano, IT";
    private static final Coordinates COORDINATES = new Coordinates(45.4716, 9.1889);
    private static final ProfileRequest REQUEST = new ProfileRequest("Demo User", 30,
            new UserAddressRequest("Via Dante 1", "Apt 4", "Milano", "Lombardia", "20121", "IT"));
    private static final ProfileResponse EXPECTED = new ProfileResponse("Demo User", 30, null);

    @Mock
    Geocoder geocoder;

    @Mock
    UserService userService;

    @InjectMocks
    UserGeocodingCoordinator coordinator;

    @Test
    void createProfile_geocodesOnlyTheAreaAndPassesTheCoordinatesToTheService() {
        given(geocoder.locate(AREA)).willReturn(Optional.of(COORDINATES));
        given(userService.createProfile(USER_SUB, REQUEST, COORDINATES)).willReturn(EXPECTED);

        then(coordinator.createProfile(USER_SUB, REQUEST)).isEqualTo(EXPECTED);
    }

    @Test
    void createProfile_whenThereAreNoCoordinates_stillCreatesTheProfile() {
        given(geocoder.locate(AREA)).willReturn(Optional.empty());
        given(userService.createProfile(USER_SUB, REQUEST, null)).willReturn(EXPECTED);

        then(coordinator.createProfile(USER_SUB, REQUEST)).isEqualTo(EXPECTED);
    }
}
