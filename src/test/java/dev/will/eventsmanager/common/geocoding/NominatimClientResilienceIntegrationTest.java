package dev.will.eventsmanager.common.geocoding;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import dev.will.eventsmanager.AbstractIntegrationTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.BDDAssertions.then;

@TestPropertySource(properties = "resilience4j.retry.instances.geocoding.wait-duration=1ms")
class NominatimClientResilienceIntegrationTest extends AbstractIntegrationTest {

    private static final String ADDRESS = "Piazza Bra 1, 37121 Verona, IT";
    private static final String FOUND = "[{\"lat\":\"45.4390\",\"lon\":\"10.9942\"}]";
    private static final Coordinates COORDINATES = new Coordinates(45.4390, 10.9942);
    private static final int MAX_ATTEMPTS = 2;

    @RegisterExtension
    static WireMockExtension nominatim = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void nominatimUrl(DynamicPropertyRegistry registry) {
        registry.add("app.geocoding.base-url", nominatim::baseUrl);
    }

    @Autowired
    NominatimClient client;

    @Autowired
    CircuitBreakerRegistry circuitBreakers;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void closeTheCircuit() {
        circuitBreaker = circuitBreakers.circuitBreaker("geocoding");
        circuitBreaker.reset();
    }

    @Test
    void shouldRetryATransientFailure() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).inScenario("flaky")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(serverError())
                .willSetStateTo("recovered"));
        nominatim.stubFor(get(urlPathEqualTo("/search")).inScenario("flaky")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson(FOUND)));

        Optional<Coordinates> result = client.locate(ADDRESS);

        then(result).contains(COORDINATES);
        nominatim.verify(MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void shouldContinueWithoutCoordinatesWhenTheProviderKeepsFailing() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(serverError()));

        Optional<Coordinates> result = client.locate(ADDRESS);

        then(result).isEmpty();
        nominatim.verify(MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void shouldNotRetryAClientError() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(badRequest()));

        Optional<Coordinates> result = client.locate(ADDRESS);

        then(result).isEmpty();
        nominatim.verify(1, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void shouldNotRetryAnAddressThatWasNotFound() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(okJson("[]")));

        Optional<Coordinates> result = client.locate(ADDRESS);

        then(result).isEmpty();
        nominatim.verify(1, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void shouldStopCallingTheProviderWhenTheCircuitOpens() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(serverError()));
        openTheCircuit();
        nominatim.resetRequests();

        Optional<Coordinates> result = client.locate(ADDRESS);

        then(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        then(result).isEmpty();
        nominatim.verify(0, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void shouldCloseTheCircuitWhenTheProviderRecovers() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(serverError()));
        openTheCircuit();
        circuitBreaker.transitionToHalfOpenState();
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(okJson(FOUND)));

        Optional<Coordinates> result = client.locate(ADDRESS);

        then(result).contains(COORDINATES);
        then(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // Each call counts twice, one per attempt, so three calls fill the window of five.
    private void openTheCircuit() {
        for (int call = 0; call < 3; call++) {
            client.locate(ADDRESS);
        }
    }
}
