package dev.will.eventsmanager.seed;

import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Transactional
@Profile("local")
class DevDataSeeder implements CommandLineRunner {

    private final EventRepository eventRepository;
    private final Faker faker = new Faker();

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) return;

        for (int i = 0; i < 10; i++) {

            EventAddress address = EventAddress.builder()
                    .street(faker.address().streetAddress())
                    .complement(faker.random().nextBoolean() ? faker.address()
                            .secondaryAddress() :
                            null)
                    .city(faker.address().city())
                    .stateOrProvince(faker.address().state())
                    .postalCode(faker.address().zipCode())
                    .countryCode("IT")
                    .venueName(faker.company().name())
                    .build();

            Event event = Event.builder()
                    .name(faker.company().name())
                    .category(Category.values()[faker.random().nextInt(Category.values().length)])
                    .schedule(LocalDateTime.now(ZoneId.systemDefault()).plusDays(faker.number().numberBetween(1, 90)))
                    .address(address)
                    .description(faker.lorem().paragraph())
                    .capacity(faker.number().numberBetween(20, 200))
                    .build();

            eventRepository.save(event);
        }
    }
}
