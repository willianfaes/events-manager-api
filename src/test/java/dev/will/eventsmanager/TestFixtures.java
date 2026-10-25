package dev.will.eventsmanager;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.entity.UserAddress;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public class TestFixtures {
    private TestFixtures() {}

    public static final String USER_SUB = "user-sub-0001";

    public static RequestPostProcessor authenticatedUser() {
        return authenticatedAs(USER_SUB);
    }

    /** A token subject for one more test user; unique per label. */
    public static String subOf(String label) {
        return "sub-" + label;
    }

    public static RequestPostProcessor authenticatedAdmin() {
        return jwt().jwt(token -> token.subject("admin-sub")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    /** A request authenticated by a token whose subject is {@code sub}, as the resource server would build it. */
    public static RequestPostProcessor authenticatedAs(String sub) {
        return jwt().jwt(token -> token.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static <T> PageResponse<T> emptyPage() {
        return PageResponse.<T>builder().content(List.of()).build();
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
                .externalId(USER_SUB)
                .address(userAddress)
                .build();
    }
}
