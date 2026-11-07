package dev.will.eventsmanager.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Two ways to authorize in Swagger UI: paste a token, or log in on the Cognito hosted page (code flow with PKCE).
    @Bean
    public OpenAPI openApi(@Value("${app.cognito.hosted-ui-url:}") String hostedUiUrl) {
        Components components = new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                .description("A Cognito access token obtained elsewhere, e.g. with the AWS CLI (see RUNNING.md).")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));

        if (!hostedUiUrl.isBlank()) {
            components.addSecuritySchemes("cognito", new SecurityScheme()
                    .description("Log in on the Cognito hosted page.")
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                            .authorizationUrl(hostedUiUrl + "/oauth2/authorize")
                            .tokenUrl(hostedUiUrl + "/oauth2/token")
                            .scopes(new Scopes()
                                    .addString("openid", "Required to log in")
                                    .addString("email", "E-mail address")
                                    .addString("profile", "Basic profile")))));
        }
        return new OpenAPI().components(components);
    }
}
