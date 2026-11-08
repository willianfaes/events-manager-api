package dev.will.eventsmanager.auth.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

// Cognito signs the ID token with the same keys and issuer as the access token, so the default checks (signature,
// issuer, expiry) accept both. The ID token is for the client that signed in, not for calling an API.
public class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error NOT_AN_ACCESS_TOKEN =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The token is not an access token", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        return "access".equals(jwt.getClaimAsString("token_use"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(NOT_AN_ACCESS_TOKEN);
    }
}
