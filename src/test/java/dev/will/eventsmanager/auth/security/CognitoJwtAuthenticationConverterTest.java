package dev.will.eventsmanager.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.BDDAssertions.then;

class CognitoJwtAuthenticationConverterTest {

    private final CognitoJwtAuthenticationConverter converter = new CognitoJwtAuthenticationConverter();

    static Stream<Arguments> groupsToAuthorities() {
        return Stream.of(
                Arguments.of(List.of("USER"), List.of("ROLE_USER")),
                Arguments.of(List.of("ADMIN"), List.of("ROLE_ADMIN")),
                Arguments.of(List.of("USER", "ADMIN"), List.of("ROLE_USER", "ROLE_ADMIN")),
                Arguments.of(List.of(), List.of()),
                // A group the code does not know never becomes a role.
                Arguments.of(List.of("OTHER", "USER"), List.of("ROLE_USER")));
    }

    @ParameterizedTest
    @MethodSource("groupsToAuthorities")
    @DisplayName("maps the cognito:groups claim to roles")
    void shouldMapGroupsToRoles(List<String> groups, List<String> expectedAuthorities) {
        Jwt jwt = jwtBuilder().claim("cognito:groups", groups).build();

        Authentication authentication = convert(jwt);

        then(authorities(authentication)).containsExactlyInAnyOrderElementsOf(expectedAuthorities);
    }

    @Test
    @DisplayName("grants no role when the token has no groups claim")
    void shouldGrantNothingWithoutGroupsClaim() {
        Authentication authentication = convert(jwtBuilder().build());

        then(authorities(authentication)).isEmpty();
    }

    @Test
    @DisplayName("names the authentication after the token subject")
    void shouldNameTheAuthenticationAfterTheSubject() {
        Authentication authentication = convert(jwtBuilder().build());

        then(authentication.getName()).isEqualTo("sub-123");
    }

    private Authentication convert(Jwt jwt) {
        return requireNonNull(converter.convert(jwt));
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-123").claim("token_use", "access");
    }

    private static List<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
