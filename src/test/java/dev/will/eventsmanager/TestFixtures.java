package dev.will.eventsmanager;

import dev.will.eventsmanager.users.entity.Role;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.entity.UserAddress;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public class TestFixtures {
    private TestFixtures() {}

    public static final String USER_EMAIL = "user@example.com";

    public static RequestPostProcessor authenticatedUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                USER_EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    public static User validUser() {
        UserAddress userAddress = UserAddress.builder()
                .street("1 Test St")
                .city("Sample City")
                .postalCode("12345")
                .countryCode("US")
                .build();

        return User.builder()
                .name("Test User")
                .email(USER_EMAIL)
                .password("irrelevant")
                .role(Role.USER)
                .address(userAddress)
                .build();
    }
}
