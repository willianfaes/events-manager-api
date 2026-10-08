package dev.will.eventsmanager.events;

import com.jayway.jsonpath.JsonPath;
import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.auth.service.TokenService;
import dev.will.eventsmanager.users.entity.Role;
import dev.will.eventsmanager.users.entity.User;
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

    @Autowired
    private TokenService tokenService;

    @Test
    void putEvents_shouldRespond405WithContractBodyWhenMethodNotSupported() throws Exception {
        String token = generateValidToken();

        MvcResult result = mockMvc.perform(put("/events")
                        .header("Authorization", "Bearer " + token))
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
        String token = generateValidToken();

        mockMvc.perform(put("/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());

        List<String> eventLines = Arrays.stream(output.getAll().split("\\R"))
                .filter(line -> line.contains(LOG_MESSAGE))
                .toList();

        assertThat(eventLines).hasSize(1);
        assertThat(eventLines.getFirst()).contains("WARN");
    }

    private String generateValidToken() {
        User user = User.builder()               // confirmar: builder/construtor
                .id(7421L)
                .email("tereza.organizer@example.com")
                .role(Role.USER)
                .build();
        return tokenService.generateToken(user);
    }
}