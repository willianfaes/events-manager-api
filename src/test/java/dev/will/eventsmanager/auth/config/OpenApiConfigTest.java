package dev.will.eventsmanager.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    @DisplayName("points the Cognito login to the hosted UI when it is configured")
    void shouldAddCognitoLoginWhenHostedUiIsConfigured() {
        OpenAPI openApi = config.openApi("https://pool.auth.eu-north-1.amazoncognito.com");

        OAuthFlow flow = openApi.getComponents().getSecuritySchemes().get("cognito").getFlows().getAuthorizationCode();
        then(flow.getAuthorizationUrl()).isEqualTo("https://pool.auth.eu-north-1.amazoncognito.com/oauth2/authorize");
        then(flow.getTokenUrl()).isEqualTo("https://pool.auth.eu-north-1.amazoncognito.com/oauth2/token");
        then(flow.getScopes()).containsOnlyKeys("openid", "email", "profile");
    }

    @Test
    @DisplayName("keeps only the pasted bearer token when the hosted UI is not configured")
    void shouldKeepOnlyBearerWithoutHostedUi() {
        OpenAPI openApi = config.openApi("");

        then(openApi.getComponents().getSecuritySchemes()).containsOnlyKeys("bearerAuth");
    }
}
