package dev.will.eventsmanager.seed;

import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.entity.UserAddress;
import dev.will.eventsmanager.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static dev.will.eventsmanager.events.entity.Category.CONCERT;
import static dev.will.eventsmanager.events.entity.Category.CULTURAL;
import static dev.will.eventsmanager.events.entity.Category.PARTY;
import static dev.will.eventsmanager.events.entity.Category.SPORT;

// Keeps the demo alive on every start: re-creates each event below when it has no upcoming date (fictional events,
// real venues) and gives the public demo user a profile when its sub is configured. The coordinates are fixed, so a
// start never calls Nominatim.
@Component
@Transactional
@Profile({"local", "prod"})
class DataSeeder implements CommandLineRunner {

    private record DemoEvent(String name, Category category, String description, int capacity, int daysAhead,
                             int startHour, String venueName, String street, String postalCode, String city,
                             String stateOrProvince, double latitude, double longitude) {
    }

    private static final String COUNTRY_CODE = "IT";

    private static final List<DemoEvent> DEMO_EVENTS = List.of(
            new DemoEvent("Night at the Museum", CULTURAL,
                    "After-hours visit to the collections with a guided tour.",
                    40, 9, 21, "Museo Egizio", "Via Accademia delle Scienze 6", "10123", "Torino", "Piemonte",
                    45.0684, 7.6844),
            new DemoEvent("Symphonic Night", CONCERT,
                    "An evening of orchestral classics.",
                    120, 12, 20, "Teatro alla Scala", "Via Filodrammatici 2", "20121", "Milano", "Lombardia",
                    45.4672, 9.1892),
            new DemoEvent("Retro 90s Party", PARTY,
                    "Dance floor hits from the nineties, all night long.",
                    250, 21, 22, "Mediolanum Forum", "Via Giuseppe di Vittorio 6", "20090", "Assago", "Lombardia",
                    45.4014, 9.1423),
            new DemoEvent("City 10K Run", SPORT,
                    "A ten kilometer city run that finishes inside the stadium.",
                    300, 30, 9, "Stadio Olimpico", "Viale dei Gladiatori 2", "00135", "Roma", "Lazio",
                    41.9309, 12.4549),
            new DemoEvent("Late Night at the Uffizi", CULTURAL,
                    "The gallery opens in the evening for a small group.",
                    60, 35, 20, "Galleria degli Uffizi", "Piazzale degli Uffizi 6", "50122", "Firenze", "Toscana",
                    43.7682, 11.2554),
            new DemoEvent("Jazz Evening", CONCERT,
                    "Live jazz quartet in a concert hall.",
                    120, 45, 21, "Auditorium Parco della Musica", "Viale Pietro de Coubertin 30", "00196", "Roma",
                    "Lazio",
                    41.9292, 12.4751),
            new DemoEvent("Open Air Night", PARTY,
                    "Open air music and dancing under the stars.",
                    200, 60, 22, "Arena di Verona", "Piazza Bra 1", "37121", "Verona", "Veneto",
                    45.4380, 10.9938),
            new DemoEvent("Charity Football Match", SPORT,
                    "Friendly match whose proceeds go to charity.",
                    150, 75, 18, "Stadio Diego Armando Maradona", "Piazzale Vincenzo Tecchio 1", "80125", "Napoli",
                    "Campania",
                    40.8241, 14.1936));

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final String demoUserSub;

    DataSeeder(EventRepository eventRepository, UserRepository userRepository,
               @Value("${app.demo.user-sub:}") String demoUserSub) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.demoUserSub = demoUserSub;
    }

    @Override
    public void run(String... args) {
        seedEvents();
        seedDemoProfile();
    }

    private void seedEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        for (DemoEvent demo : DEMO_EVENTS) {
            if (eventRepository.existsByNameAndScheduleAfter(demo.name(), now)) continue;

            EventAddress address = EventAddress.builder()
                    .street(demo.street())
                    .city(demo.city())
                    .stateOrProvince(demo.stateOrProvince())
                    .postalCode(demo.postalCode())
                    .countryCode(COUNTRY_CODE)
                    .venueName(demo.venueName())
                    .latitude(demo.latitude())
                    .longitude(demo.longitude())
                    .build();

            eventRepository.save(Event.builder()
                    .name(demo.name())
                    .category(demo.category())
                    .schedule(today.plusDays(demo.daysAhead()).atTime(demo.startHour(), 0))
                    .address(address)
                    .description(demo.description())
                    .capacity(demo.capacity())
                    .build());
        }
    }

    // Milano's coordinates, as geocoding the postal code, city and country would give them.
    private void seedDemoProfile() {
        if (demoUserSub.isBlank() || userRepository.findByExternalId(demoUserSub).isPresent()) return;

        UserAddress address = UserAddress.builder()
                .street("Piazza del Duomo")
                .city("Milano")
                .stateOrProvince("Lombardia")
                .postalCode("20122")
                .countryCode(COUNTRY_CODE)
                .latitude(45.4642)
                .longitude(9.1896)
                .build();

        userRepository.save(User.builder()
                .name("Demo User")
                .age(30)
                .externalId(demoUserSub)
                .address(address)
                .build());
    }
}
