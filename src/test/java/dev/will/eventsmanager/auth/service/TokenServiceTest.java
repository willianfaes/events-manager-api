package dev.will.eventsmanager.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import dev.will.eventsmanager.auth.security.JWTUserData;
import dev.will.eventsmanager.users.entity.Role;
import dev.will.eventsmanager.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

class TokenServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes!!";
    private static final String EMAIL = "user@example.com";

    private final TokenService tokenService = new TokenService(SECRET);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "too-short", "31-bytes-long-secret-aaaaaaaaaa"})
    @DisplayName("should refuse to start when the secret is missing, blank or shorter than 32 bytes")
    void constructor_whenSecretIsMissingOrTooShort_throwsIllegalArgumentException(String secret) {
        thenThrownBy(() -> new TokenService(secret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    @DisplayName("should accept a secret of exactly 32 bytes")
    void constructor_whenSecretHas32Bytes_isAccepted() {
        then(new TokenService("32-bytes-long-secret-aaaaaaaaaaa")).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("should return the e-mail and role of a freshly generated token")
    void validateToken_whenTokenWasGeneratedBySameService_returnsEmailAndRole(Role role) {
        String token = tokenService.generateToken(user(role));

        Optional<JWTUserData> result = tokenService.validateToken(token);

        then(result).isPresent();
        then(result.get().email()).isEqualTo(EMAIL);
        then(result.get().role()).isEqualTo(role);
    }

    @Test
    @DisplayName("should reject a token signed with another secret")
    void validateToken_whenSignedWithAnotherSecret_returnsEmpty() {
        String forged = JWT.create()
                .withSubject(EMAIL)
                .withClaim("role", Role.ADMIN.name())
                .withExpiresAt(Instant.now().plusSeconds(60))
                .sign(Algorithm.HMAC256("another-secret-with-at-least-32-bytes"));

        then(tokenService.validateToken(forged)).isEmpty();
    }

    @Test
    @DisplayName("should reject an expired token")
    void validateToken_whenTokenIsExpired_returnsEmpty() {
        String expired = JWT.create()
                .withSubject(EMAIL)
                .withClaim("role", Role.USER.name())
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SECRET));

        then(tokenService.validateToken(expired)).isEmpty();
    }

    @Test
    @DisplayName("should reject a valid token that has no role claim")
    void validateToken_whenRoleClaimIsMissing_returnsEmpty() {
        String withoutRole = JWT.create()
                .withSubject(EMAIL)
                .withExpiresAt(Instant.now().plusSeconds(60))
                .sign(Algorithm.HMAC256(SECRET));

        then(tokenService.validateToken(withoutRole)).isEmpty();
    }

    @Test
    @DisplayName("should reject a string that is not a JWT")
    void validateToken_whenTokenIsGarbage_returnsEmpty() {
        then(tokenService.validateToken("not-a-jwt")).isEmpty();
    }

    private static User user(Role role) {
        return User.builder().email(EMAIL).role(role).build();
    }
}
