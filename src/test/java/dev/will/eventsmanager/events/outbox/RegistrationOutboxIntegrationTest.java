package dev.will.eventsmanager.events.outbox;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.SharedKafka;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.repository.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static dev.will.eventsmanager.TestFixtures.USER_SUB;
import static dev.will.eventsmanager.TestFixtures.authenticatedUser;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// From the API to the broker: registration, outbox row, relay, Kafka. The relay is called directly (scheduling is off
// in tests), so the test does not depend on timing. The database rolls back; the Kafka message stays, found by its key.
@Transactional
class RegistrationOutboxIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        SharedKafka.register(registry);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    OutboxMessageRepository outboxRepository;

    @Autowired
    OutboxRelay relay;

    @Test
    @DisplayName("POST /events/{eventId}/registrations - should publish a REGISTRATION_CREATED message keyed by the event")
    void register_publishesTheRegistrationToKafka() throws Exception {
        userRepository.save(validUser());
        Event event = saveEvent();
        String key = event.getId().toString();

        mockMvc.perform(post("/events/{eventId}/registrations", event.getId()).with(authenticatedUser()))
                .andExpect(status().isCreated());
        relay.publishPending();

        ConsumerRecord<String, String> consumerRecord = SharedKafka.readByKey(KafkaRegistrationEventPublisher.TOPIC, key);
        OutboxMessage row = outboxRepository.findAll().stream()
                .filter(message -> message.getMessageKey().equals(key))
                .findFirst().orElseThrow();
        // Lenient: occurredAt is also in the payload, with the current time.
        JSONAssert.assertEquals("""
                {"messageId": "%s", "type": "REGISTRATION_CREATED", "eventId": %s, "userSub": "%s"}
                """.formatted(row.getMessageId(), key, USER_SUB), consumerRecord.value(), false);
        then(row.isSent()).isTrue();
    }

    private Event saveEvent() {
        EventAddress address = EventAddress.builder()
                .street("123 Main St")
                .city("Sample City")
                .postalCode("12345")
                .countryCode("US")
                .venueName("Sample Venue")
                .build();
        return eventRepository.save(Event.builder()
                .name("Outbox Event")
                .category(Category.CONCERT)
                .schedule(LocalDateTime.of(2126, 10, 1, 10, 0))
                .address(address)
                .capacity(10)
                .build());
    }
}
