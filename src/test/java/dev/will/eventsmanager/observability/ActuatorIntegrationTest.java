package dev.will.eventsmanager.observability;

import dev.will.eventsmanager.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.BDDAssertions.then;

@AutoConfigureObservability
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
class ActuatorIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TestRestTemplate http;

    private String management(String path) {
        return "http://localhost:" + managementPort + path;
    }

    @Test
    @DisplayName("GET /actuator/health without a token returns UP and no component details")
    void healthIsPublicAndHidesDetails() {
        ResponseEntity<String> response = http.getForEntity(management("/actuator/health"), String.class);

        then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(response.getBody()).contains("\"status\":\"UP\"").doesNotContain("components");
    }

    @Test
    @DisplayName("GET /actuator/prometheus on the management port returns JVM and Hikari metrics")
    void prometheusExposesMetrics() {
        ResponseEntity<String> response = http.getForEntity(management("/actuator/prometheus"), String.class);

        then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(response.getBody()).contains("jvm_memory_used_bytes", "hikaricp_connections");
    }

    @Test
    @DisplayName("Actuator endpoints other than health and prometheus are not accessible")
    void otherEndpointsAreNotAccessible() {
        ResponseEntity<String> response = http.getForEntity(management("/actuator/env"), String.class);

        then(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /actuator/prometheus on the application port is not reachable")
    void prometheusIsNotOnTheApplicationPort() {
        ResponseEntity<String> response =
                http.getForEntity("http://localhost:" + serverPort + "/actuator/prometheus", String.class);

        then(response.getStatusCode().is2xxSuccessful()).isFalse();
    }
}
