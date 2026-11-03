package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.AbstractJpaTest;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static dev.will.eventsmanager.TestFixtures.subOf;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.assertj.core.api.Assertions.assertThat;

class EventRegistrationsRepositoryJpaTest extends AbstractJpaTest {

    private static final String ANA = "ana";

    @Autowired
    EventRegistrationsRepository registrationsRepository;

    @Autowired
    TestEntityManager em;

    @Test
    void findRegisteredEventIds_shouldReturnOnlyTheIdsTheUserIsRegisteredFor() {
        User ana = saveUser(ANA);
        Event first = saveEvent("First");
        Event second = saveEvent("Second");
        Event third = saveEvent("Third");
        saveRegistration(ana, first);
        saveRegistration(ana, third);
        em.clear();

        List<Long> result = registrationsRepository.findRegisteredEventIds(
                subOf(ANA), List.of(first.getId(), second.getId(), third.getId()));

        assertThat(result).containsExactly(first.getId(), third.getId());
    }

    @Test
    void findRegisteredEventIds_shouldIgnoreCancelledRegistrations() {
        User ana = saveUser(ANA);
        Event event = saveEvent("Cancelled");
        EventRegistration registration = saveRegistration(ana, event);
        registrationsRepository.delete(registration);
        em.flush();
        em.clear();

        List<Long> result = registrationsRepository.findRegisteredEventIds(subOf(ANA), List.of(event.getId()));

        assertThat(result).isEmpty();
    }

    @Test
    void findRegisteredEventIds_shouldIgnoreIdsThatMatchNoEvent() {
        User ana = saveUser(ANA);
        Event event = saveEvent("Known");
        saveRegistration(ana, event);
        em.clear();

        List<Long> result = registrationsRepository.findRegisteredEventIds(
                subOf(ANA), List.of(event.getId(), 999_999L));

        assertThat(result).containsExactly(event.getId());
    }

    @Test
    void findRegisteredEventIds_shouldNotReturnRegistrationsOfOtherUsers() {
        User ana = saveUser(ANA);
        User bruno = saveUser("bruno");
        Event shared = saveEvent("Shared");
        Event onlyBruno = saveEvent("Only Bruno");
        saveRegistration(ana, shared);
        saveRegistration(bruno, shared);
        saveRegistration(bruno, onlyBruno);
        em.clear();

        List<Long> result = registrationsRepository.findRegisteredEventIds(
                subOf(ANA), List.of(shared.getId(), onlyBruno.getId()));

        assertThat(result).containsExactly(shared.getId());
    }

    @Test
    void countByEventIds_shouldCountTheActiveRegistrationsOfEachRequestedEvent() {
        User ana = saveUser(ANA);
        User bruno = saveUser("bruno");
        Event twoRegistrations = saveEvent("Two");
        Event oneRegistration = saveEvent("One");
        Event noRegistrations = saveEvent("None");
        Event notRequested = saveEvent("Not requested");
        saveRegistration(ana, twoRegistrations);
        saveRegistration(bruno, twoRegistrations);
        saveRegistration(ana, oneRegistration);
        saveRegistration(ana, notRequested);
        em.clear();

        List<EventRegistrationCount> result = registrationsRepository.countByEventIds(
                List.of(twoRegistrations.getId(), oneRegistration.getId(), noRegistrations.getId()));

        assertThat(result).containsExactlyInAnyOrder(
                new EventRegistrationCount(twoRegistrations.getId(), 2L),
                new EventRegistrationCount(oneRegistration.getId(), 1L));
    }

    @Test
    void countByEventIds_shouldNotCountCancelledRegistrations() {
        User ana = saveUser(ANA);
        User bruno = saveUser("bruno");
        Event event = saveEvent("Partly cancelled");
        saveRegistration(ana, event);
        EventRegistration cancelled = saveRegistration(bruno, event);
        registrationsRepository.delete(cancelled);
        em.flush();
        em.clear();

        List<EventRegistrationCount> result = registrationsRepository.countByEventIds(List.of(event.getId()));

        assertThat(result).containsExactly(new EventRegistrationCount(event.getId(), 1L));
    }

    private User saveUser(String label) {
        User user = validUser();
        user.setExternalId(subOf(label));
        return em.persistAndFlush(user);
    }

    private Event saveEvent(String name) {
        EventAddress address = EventAddress.builder()
                .street("Via Roma 1")
                .city("Torino")
                .postalCode("10100")
                .countryCode("IT")
                .build();

        Event event = Event.builder()
                .name(name)
                .category(Category.PARTY)
                .schedule(LocalDateTime.now().plusDays(30))
                .address(address)
                .capacity(100)
                .build();

        return em.persistAndFlush(event);
    }

    private EventRegistration saveRegistration(User user, Event event) {
        return em.persistAndFlush(EventRegistration.builder().user(user).event(event).build());
    }
}
