package dev.will.eventsmanager.seed;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@Transactional
class DataSeederIntegrationTest extends AbstractIntegrationTest {
    private static final String JAZZ_EVENING = "Jazz Evening";
    private static final List<String> DEMO_EVENT_NAMES = List.of(
            "Night at the Museum",
            "Symphonic Night",
            "Retro 90s Party",
            "City 10K Run",
            "Late Night at the Uffizi",
            JAZZ_EVENING,
            "Open Air Night",
            "Charity Football Match");

    @Autowired
    DataSeeder seeder;

    @Autowired
    EventRepository eventRepository;

    // The context already ran the seeder once and committed its events; start from an empty table (rolled back).
    @BeforeEach
    void emptyEventsTable() {
        eventRepository.deleteAll();
    }

    @Test
    void run_whenDatabaseIsEmpty_createsTheDemoEvents() {
        LocalDateTime before = LocalDateTime.now();

        seeder.run();

        List<Event> events = eventRepository.findAll();
        then(events).extracting(Event::getName).containsExactlyInAnyOrderElementsOf(DEMO_EVENT_NAMES);
        then(events).extracting(Event::getSchedule)
                .allSatisfy(schedule -> then(schedule).isBetween(before.plusDays(8), before.plusDays(76)));
        then(events).extracting(Event::getAddress).extracting(EventAddress::getCountryCode).containsOnly("IT");
        then(events).extracting(Event::getAddress).extracting(EventAddress::getLatitude).doesNotContainNull();
        then(events).extracting(Event::getAddress).extracting(EventAddress::getLongitude).doesNotContainNull();
    }

    @Test
    void run_whenTheSeedWasAlreadyApplied_doesNotDuplicateEvents() {
        seeder.run();

        seeder.run();

        then(eventRepository.findAll()).hasSize(DEMO_EVENT_NAMES.size());
    }

    @Test
    void run_whenADemoEventHasPassed_createsItAgainInTheFuture() {
        seeder.run();
        Event jazz = eventsNamed(JAZZ_EVENING).getFirst();
        jazz.setSchedule(LocalDateTime.now().minusDays(1));
        eventRepository.saveAndFlush(jazz);

        seeder.run();

        List<Event> jazzEvents = eventsNamed(JAZZ_EVENING);
        then(jazzEvents).hasSize(2);
        then(jazzEvents).filteredOn(event -> event.getSchedule().isAfter(LocalDateTime.now())).hasSize(1);
    }

    private List<Event> eventsNamed(String name) {
        return eventRepository.findAll().stream().filter(event -> event.getName().equals(name)).toList();
    }
}
