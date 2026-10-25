package dev.will.eventsmanager.users.repository;

import dev.will.eventsmanager.TestFixtures;
import dev.will.eventsmanager.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryJpaTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("finds the user linked to an identity provider id")
    void shouldFindUserByExternalId() {
        User user = TestFixtures.validUser();
        user.setExternalId("sub-123");
        userRepository.saveAndFlush(user);

        then(userRepository.findByExternalId("sub-123")).get().extracting(User::getName)
                .isEqualTo(user.getName());
        then(userRepository.findByExternalId("sub-999")).isEmpty();
    }

    @Test
    @DisplayName("rejects two users linked to the same identity provider id")
    void shouldRejectDuplicatedExternalId() {
        User first = TestFixtures.validUser();
        first.setExternalId("sub-123");
        userRepository.saveAndFlush(first);

        User second = TestFixtures.validUser();
        second.setExternalId("sub-123");

        thenThrownBy(() -> userRepository.saveAndFlush(second)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
