package dev.will.eventsmanager.users.repository;

import dev.will.eventsmanager.AbstractJpaTest;
import dev.will.eventsmanager.TestFixtures;
import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

class UserRepositoryJpaTest extends AbstractJpaTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("keeps the coordinates of the profile address")
    void shouldPersistAddressCoordinates() {
        User user = TestFixtures.validUser();
        user.getAddress().locateAt(new Coordinates(45.4642, 9.19));
        userRepository.saveAndFlush(user);
        entityManager.clear();

        User found = userRepository.findByExternalId(user.getExternalId()).orElseThrow();

        then(found.getAddress().getLatitude()).isEqualTo(45.4642);
        then(found.getAddress().getLongitude()).isEqualTo(9.19);
    }

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
