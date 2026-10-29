package dev.will.eventsmanager.auth.config;

import dev.will.eventsmanager.events.service.EventService;
import dev.will.eventsmanager.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The access rules (who needs a token, who needs which role) are tested here, once, over every controller; the
// controller slices only test what their routes do. A widened permitAll or a route left open must fail a test here.
@WebMvcTest
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class SecurityAccessRulesWebSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("protected routes - should answer 401 in JSON when there is no token")
    @CsvSource({
            "GET,/events",
            "GET,/events/1",
            "POST,/events/1/registrations",
            "GET,/events/1/registrations/me",
            "DELETE,/events/1/registrations",
            "POST,/users/me",
            "GET,/users/me",
            "GET,/users/me/registrations",
            "GET,/users/me/registrations/events",
            "POST,/admin/events",
            "PUT,/admin/events/1",
            "DELETE,/admin/events/1",
            "GET,/admin/events/1/registrations"
    })
    void protectedRoute_withoutToken_returnsUnauthorized(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest(name = "GET {0}")
    @DisplayName("public routes - should not ask for a token")
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html"})
    void publicRoute_withoutToken_isNotRejected(String path) throws Exception {
        int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();

        then(status).isNotEqualTo(401);
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("routes next to the public ones - should answer 401 when there is no token")
    @CsvSource({
            "POST,/v3/api-docs",
            "PUT,/swagger-ui.html",
            "GET,/auth/login",
            "GET,/actuator/env",
            "GET,/unknown"
    })
    void siblingRoute_withoutToken_returnsUnauthorized(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isUnauthorized());
    }
}
