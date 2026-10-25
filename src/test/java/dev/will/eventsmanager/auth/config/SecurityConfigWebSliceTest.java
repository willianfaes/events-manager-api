package dev.will.eventsmanager.auth.config;

import dev.will.eventsmanager.events.controller.AdminEventController;
import dev.will.eventsmanager.events.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Goes through the real Bearer token filter: only the decoder is mocked, so the groups are mapped by our converter.
@WebMvcTest(AdminEventController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class SecurityConfigWebSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("lets a token in the ADMIN group into /admin")
    void shouldAllowAdminGroup() throws Exception {
        given(jwtDecoder.decode("admin-token")).willReturn(tokenInGroup("ADMIN"));

        mockMvc.perform(get("/admin/events/1/registrations").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("answers 403 to a token in the USER group on /admin")
    void shouldForbidUserGroup() throws Exception {
        given(jwtDecoder.decode("user-token")).willReturn(tokenInGroup("USER"));

        mockMvc.perform(get("/admin/events/1/registrations").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("answers 401 in JSON when the token is rejected (expired, other issuer, bad signature)")
    void shouldRejectInvalidToken() throws Exception {
        given(jwtDecoder.decode("expired-token")).willThrow(new BadJwtException("Jwt expired"));

        mockMvc.perform(get("/admin/events/1/registrations").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private static Jwt tokenInGroup(String group) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-123")
                .claim("cognito:groups", List.of(group)).build();
    }
}
