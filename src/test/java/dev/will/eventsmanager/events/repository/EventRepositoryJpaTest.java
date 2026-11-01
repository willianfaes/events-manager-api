package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.SharedPostgres;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryJpaTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
    }

    private static final Pageable PAGEABLE = PageRequest.of(0, 20, Sort.by("schedule"));
    private static final LocalDate DAY = LocalDate.now().plusDays(30);

    @Autowired
    EventRepository eventRepository;

    @Autowired
    TestEntityManager em;

    // The seeder commits its events in the integration tests, and the database is shared: hide them in this
    // transaction, which rolls back.
    @BeforeEach
    void clearSeededEvents() {
        eventRepository.deleteAll();
    }

    @Test
    void shouldFilterByNameIgnoringCase() {
        saveEvent("Rock in Rio", DAY.atTime(20, 0));
        saveEvent("Jazz Night", DAY.atTime(21, 0));
        em.clear();
        EventFilter filter = new EventFilter("ROCK", null, null);

        Page<Event> result = eventRepository.findAll(EventSpecs.from(filter), PAGEABLE);

        assertThat(result.getContent())
                .extracting(Event::getName)
                .containsExactly("Rock in Rio");
    }

    @Test
    void shouldIncludeFirstMomentOfFromDay() {
        saveEvent("Before", DAY.minusDays(1).atTime(23, 59));
        saveEvent("On from day", DAY.atStartOfDay());
        em.clear();
        EventFilter filter = new EventFilter(null, DAY, null);

        Page<Event> result = eventRepository.findAll(EventSpecs.from(filter), PAGEABLE);

        assertThat(result.getContent())
                .extracting(Event::getName)
                .containsExactly("On from day");
    }

    @Test
    void shouldIncludeWholeLastDay() {
        saveEvent("Evening of to day", DAY.atTime(18, 0));
        saveEvent("Next day", DAY.plusDays(1).atStartOfDay());
        em.clear();
        EventFilter filter = new EventFilter(null, null, DAY);

        Page<Event> result = eventRepository.findAll(EventSpecs.from(filter), PAGEABLE);

        assertThat(result.getContent())
                .extracting(Event::getName)
                .containsExactly("Evening of to day");
    }

    @Test
    void shouldHidePastEventsWhenFromIsMissing() {
        saveEvent("Past", LocalDateTime.now().minusDays(10));
        saveEvent("Future", LocalDateTime.now().plusDays(10));
        em.clear();
        EventFilter filter = new EventFilter(null, null, null);

        Page<Event> result = eventRepository.findAll(EventSpecs.from(filter), PAGEABLE);

        assertThat(result.getContent())
                .extracting(Event::getName)
                .containsExactly("Future");
    }

    @Test
    void shouldFetchAddressTogetherWithEvents() {
        saveEvent("Any", DAY.atTime(20, 0));
        em.clear();
        EventFilter filter = new EventFilter(null, null, null);

        Page<Event> result = eventRepository.findAll(EventSpecs.from(filter), PAGEABLE);

        assertThat(Hibernate.isInitialized(result.getContent().getFirst().getAddress())).isTrue();
    }

    @Test
    void shouldPersistAddressCoordinates() {
        EventAddress address = EventAddress.builder()
                .street("Via Roma 1")
                .city("Torino")
                .postalCode("10100")
                .countryCode("IT")
                .latitude(45.0703)
                .longitude(7.6869)
                .build();
        Long id = em.persistFlushFind(address).getId();
        em.clear();

        EventAddress found = em.find(EventAddress.class, id);

        assertThat(found.getLatitude()).isEqualTo(45.0703);
        assertThat(found.getLongitude()).isEqualTo(7.6869);
    }

    private void saveEvent(String name, LocalDateTime schedule) {
        EventAddress address = EventAddress.builder()
                .street("Via Roma 1")
                .city("Torino")
                .postalCode("10100")
                .countryCode("IT")
                .build();

        Event event = Event.builder()
                .name(name)
                .category(Category.PARTY)
                .schedule(schedule)
                .address(address)
                .capacity(100)
                .build();

        em.persistAndFlush(event);
    }
}
