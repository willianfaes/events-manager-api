package dev.will.eventsmanager.common.geocoding;

import dev.will.eventsmanager.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GeocoderCacheIntegrationTest extends AbstractIntegrationTest {

    private static final String ADDRESS = "Piazza Bra 1, 37121 Verona, IT";
    private static final Coordinates COORDINATES = new Coordinates(45.4390, 10.9942);

    @Autowired
    Geocoder geocoder;

    @MockitoBean
    NominatimClient nominatimClient;

    @Test
    void shouldCallTheProviderOnceForTheSameAddress() {
        given(nominatimClient.locate(ADDRESS)).willReturn(Optional.of(COORDINATES));

        Optional<Coordinates> first = geocoder.locate(ADDRESS);
        Optional<Coordinates> second = geocoder.locate(ADDRESS);

        then(first).contains(COORDINATES);
        then(second).contains(COORDINATES);
        verify(nominatimClient, times(1)).locate(anyString());
    }

    @Test
    void shouldTreatCaseAndOuterSpacesAsTheSameAddress() {
        given(nominatimClient.locate(ADDRESS)).willReturn(Optional.of(COORDINATES));

        geocoder.locate(ADDRESS);
        Optional<Coordinates> second = geocoder.locate("  " + ADDRESS.toUpperCase() + " ");

        then(second).contains(COORDINATES);
        verify(nominatimClient, times(1)).locate(anyString());
    }

    @Test
    void shouldNotCacheAnAddressThatWasNotFound() {
        given(nominatimClient.locate(ADDRESS)).willReturn(Optional.empty());

        geocoder.locate(ADDRESS);
        Optional<Coordinates> second = geocoder.locate(ADDRESS);

        then(second).isEmpty();
        verify(nominatimClient, times(2)).locate(anyString());
    }
}
