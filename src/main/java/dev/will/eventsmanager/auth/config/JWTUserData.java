package dev.will.eventsmanager.auth.config;

import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@Builder
public record JWTUserData(Long userId, String email) {
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
