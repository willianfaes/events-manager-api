package dev.will.eventsmanager.events.entity;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.BDDAssertions.then;

class EventAddressTest {

    private static final Coordinates VERONA = new Coordinates(45.4390, 10.9942);
    private static final Coordinates ROMA = new Coordinates(41.9028, 12.4964);

    @Test
    @DisplayName("relocate saves the new coordinates when the lookup succeeds, even on the same address")
    void relocate_whenTheLookupSucceeds_savesTheNewCoordinates() {
        EventAddress address = addressInVerona();
        List<String> fieldsBefore = address.geocodedFields();

        address.relocate(ROMA, fieldsBefore);

        then(address.coordinates()).isEqualTo(ROMA);
    }

    @Test
    @DisplayName("relocate keeps the coordinates when the lookup fails and the address did not change")
    void relocate_whenTheLookupFailsOnTheSameAddress_keepsTheCoordinates() {
        EventAddress address = addressInVerona();
        List<String> fieldsBefore = address.geocodedFields();

        address.relocate(null, fieldsBefore);

        then(address.coordinates()).isEqualTo(VERONA);
    }

    @DisplayName("relocate clears the coordinates when the lookup fails and a geocoded field changed")
    @ParameterizedTest(name = "{0} changed")
    @ValueSource(strings = {"street", "postalCode", "city", "countryCode"})
    void relocate_whenTheLookupFailsOnAChangedAddress_clearsTheCoordinates(String field) {
        EventAddress address = addressInVerona();
        List<String> fieldsBefore = address.geocodedFields();
        change(field).accept(address);

        address.relocate(null, fieldsBefore);

        then(address.coordinates()).isNull();
    }

    @Test
    @DisplayName("relocate keeps the coordinates when the lookup fails and only a field outside geocoding changed")
    void relocate_whenOnlyTheComplementChanged_keepsTheCoordinates() {
        EventAddress address = addressInVerona();
        List<String> fieldsBefore = address.geocodedFields();
        address.setComplement("Gate 3");

        address.relocate(null, fieldsBefore);

        then(address.coordinates()).isEqualTo(VERONA);
    }

    private static EventAddress addressInVerona() {
        EventAddress address = EventAddress.builder()
                .street("Piazza Bra 1")
                .postalCode("37121")
                .city("Verona")
                .countryCode("IT")
                .build();
        address.locateAt(VERONA);
        return address;
    }

    private static Consumer<EventAddress> change(String field) {
        return switch (field) {
            case "street" -> address -> address.setStreet("Via Roma 2");
            case "postalCode" -> address -> address.setPostalCode("37122");
            case "city" -> address -> address.setCity("Vicenza");
            case "countryCode" -> address -> address.setCountryCode("CH");
            default -> throw new IllegalArgumentException(field);
        };
    }
}
