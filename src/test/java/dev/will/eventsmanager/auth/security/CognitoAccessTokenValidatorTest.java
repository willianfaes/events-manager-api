package dev.will.eventsmanager.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.BDDAssertions.then;

class CognitoAccessTokenValidatorTest {

    private final CognitoAccessTokenValidator validator = new CognitoAccessTokenValidator();

    @Test
    @DisplayName("accepts an access token")
    void shouldAcceptAnAccessToken() {
        OAuth2TokenValidatorResult result = validator.validate(jwtBuilder().claim("token_use", "access").build());

        then(result.hasErrors()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "ACCESS", ""})
    @DisplayName("rejects any other token_use, such as an ID token")
    void shouldRejectAnyOtherTokenUse(String tokenUse) {
        OAuth2TokenValidatorResult result = validator.validate(jwtBuilder().claim("token_use", tokenUse).build());

        then(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("rejects a token without token_use")
    void shouldRejectATokenWithoutTokenUse() {
        OAuth2TokenValidatorResult result = validator.validate(jwtBuilder().build());

        then(result.hasErrors()).isTrue();
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-123");
    }
}
