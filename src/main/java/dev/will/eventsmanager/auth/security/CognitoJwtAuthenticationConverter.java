package dev.will.eventsmanager.auth.security;

import dev.will.eventsmanager.users.entity.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String GROUPS_CLAIM = "cognito:groups";

    private static final Set<String> ROLES = Arrays.stream(Role.values()).map(Role::name).collect(Collectors.toSet());

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList(GROUPS_CLAIM);
        List<GrantedAuthority> authorities = groups == null ? List.of() : groups.stream()
                // A group created in the pool that is not a Role never becomes an authority.
                .filter(ROLES::contains)
                .<GrantedAuthority>map(group -> new SimpleGrantedAuthority("ROLE_" + group))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
