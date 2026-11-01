package dev.will.eventsmanager.common.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Turns an address into coordinates. An unknown address is an empty result; a failing provider is an exception,
 * so that only the failures can be retried.
 */
@Component
public class NominatimClient {

    private final RestClient restClient;

    public NominatimClient(RestClient.Builder builder,
                           @Value("${app.geocoding.base-url}") String baseUrl,
                           @Value("${app.geocoding.user-agent}") String userAgent,
                           @Value("${app.geocoding.connect-timeout}") Duration connectTimeout,
                           @Value("${app.geocoding.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<Coordinates> locate(String address) {
        List<Place> places = restClient.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("q", address)
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return places == null || places.isEmpty()
                ? Optional.empty()
                : Optional.of(places.getFirst().toCoordinates());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Place(double lat, double lon) {

        private Coordinates toCoordinates() {
            return new Coordinates(lat, lon);
        }
    }
}
