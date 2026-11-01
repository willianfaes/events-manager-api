package dev.will.eventsmanager.common.geocoding;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

class NominatimClientTest {

    private static final String ADDRESS = "Piazza Bra 1, 37121 Verona, IT";
    private static final String USER_AGENT = "events-manager-test";
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

    @RegisterExtension
    static WireMockExtension nominatim = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private NominatimClient client;

    @BeforeEach
    void setUp() {
        client = new NominatimClient(RestClient.builder(), nominatim.baseUrl(), USER_AGENT,
                Duration.ofSeconds(2), READ_TIMEOUT);
    }

    @Test
    void shouldReturnCoordinatesOfTheFirstResult() {
        nominatim.stubFor(get(urlPathEqualTo("/search"))
                .willReturn(okJson("[{\"lat\":\"45.4390\",\"lon\":\"10.9942\"}]")));

        Optional<Coordinates> result = client.locate(ADDRESS);

        then(result).contains(new Coordinates(45.4390, 10.9942));
    }

    @Test
    void shouldSendTheAddressAndIdentifyTheApplication() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(okJson("[]")));

        client.locate(ADDRESS);

        nominatim.verify(getRequestedFor(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo(ADDRESS))
                .withQueryParam("format", equalTo("jsonv2"))
                .withQueryParam("limit", equalTo("1"))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void shouldReturnEmptyWhenTheAddressIsNotFound() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(okJson("[]")));

        then(client.locate(ADDRESS)).isEmpty();
    }

    @Test
    void shouldThrowWhenTheProviderFails() {
        nominatim.stubFor(get(urlPathEqualTo("/search")).willReturn(serverError()));

        thenThrownBy(() -> client.locate(ADDRESS)).isInstanceOf(RestClientException.class);
    }

    @Test
    void shouldThrowWhenTheProviderIsTooSlow() {
        nominatim.stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withFixedDelay((int) READ_TIMEOUT.plusMillis(500).toMillis()).withStatus(200).withBody("[]")));

        thenThrownBy(() -> client.locate(ADDRESS)).isInstanceOf(RestClientException.class);
    }
}
