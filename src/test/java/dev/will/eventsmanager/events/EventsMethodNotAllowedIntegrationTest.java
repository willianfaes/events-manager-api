package dev.will.eventsmanager.events;

import com.jayway.jsonpath.JsonPath;
import dev.will.eventsmanager.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class EventsMethodNotAllowedIntegrationTest extends AbstractIntegrationTest {

    private static final String LOG_MESSAGE = "HTTP method not supported at /events";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void putEvents_shouldRespond405WithContractBodyWhenMethodNotSupported() throws Exception {
        MvcResult result = mockMvc.perform(put("/events")
                        .with(authenticatedUser()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        Map<String, Object> body = JsonPath.read(result.getResponse().getContentAsString(), "$");

        assertThat(body)
                .containsOnlyKeys("timestamp", "status", "error", "message", "path")
                .containsEntry("status", 405)
                .containsEntry("error", "Method Not Allowed")
                .containsEntry("message", "HTTP method not allowed")
                .containsEntry("path", "/events");
        assertThatCode(() -> Instant.parse((String) body.get("timestamp")))
                .doesNotThrowAnyException();
    }

    @Test
    void putEvents_shouldLogWarnWhenMethodNotSupported(CapturedOutput output) throws Exception {
        mockMvc.perform(put("/events")
                        .with(authenticatedUser()))
                .andExpect(status().isMethodNotAllowed());

        List<String> eventLines = Arrays.stream(output.getAll().split("\\R"))
                .filter(line -> line.contains(LOG_MESSAGE))
                .toList();

        assertThat(eventLines).hasSize(1);
        assertThat(eventLines.getFirst()).contains("WARN");
    }
}