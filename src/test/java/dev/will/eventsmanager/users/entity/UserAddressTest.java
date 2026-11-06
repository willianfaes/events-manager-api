package dev.will.eventsmanager.users.entity;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.BDDAssertions.then;

class UserAddressTest {

    private static final Coordinates MILANO = new Coordinates(45.4642, 9.1900);
    private static final Coordinates TORINO = new Coordinates(45.0703, 7.6869);

    @Test
    @DisplayName("relocate saves the new coordinates when the lookup succeeds, even on the same area")
    void relocate_whenTheLookupSucceeds_savesTheNewCoordinates() {
        UserAddress address = addressInMilano();
        List<String> fieldsBefore = address.geocodedFields();

        address.relocate(TORINO, fieldsBefore);

        then(address.coordinates()).isEqualTo(TORINO);
    }

    @Test
    @DisplayName("relocate keeps the coordinates when the lookup fails and the area did not change")
    void relocate_whenTheLookupFailsOnTheSameArea_keepsTheCoordinates() {
        UserAddress address = addressInMilano();
        List<String> fieldsBefore = address.geocodedFields();

        address.relocate(null, fieldsBefore);

        then(address.coordinates()).isEqualTo(MILANO);
    }

    @DisplayName("relocate clears the coordinates when the lookup fails and a geocoded field changed")
    @ParameterizedTest(name = "{0} changed")
    @ValueSource(strings = {"postalCode", "city", "countryCode"})
    void relocate_whenTheLookupFailsOnAChangedArea_clearsTheCoordinates(String field) {
        UserAddress address = addressInMilano();
        List<String> fieldsBefore = address.geocodedFields();
        change(field).accept(address);

        address.relocate(null, fieldsBefore);

        then(address.coordinates()).isNull();
    }

    @Test
    @DisplayName("relocate keeps the coordinates when the lookup fails and only the street changed, since it is never geocoded")
    void relocate_whenOnlyTheStreetChanged_keepsTheCoordinates() {
        UserAddress address = addressInMilano();
        List<String> fieldsBefore = address.geocodedFields();
        address.setStreet("Via Torino 5");

        address.relocate(null, fieldsBefore);

        then(address.coordinates()).isEqualTo(MILANO);
    }

    private static UserAddress addressInMilano() {
        UserAddress address = UserAddress.builder()
                .street("Via Dante 1")
                .postalCode("20121")
                .city("Milano")
                .countryCode("IT")
                .build();
        address.locateAt(MILANO);
        return address;
    }

    private static Consumer<UserAddress> change(String field) {
        return switch (field) {
            case "postalCode" -> address -> address.setPostalCode("20122");
            case "city" -> address -> address.setCity("Monza");
            case "countryCode" -> address -> address.setCountryCode("CH");
            default -> throw new IllegalArgumentException(field);
        };
    }
}
