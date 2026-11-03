package dev.will.eventsmanager.events.outbox;

import dev.will.eventsmanager.common.config.KafkaConfig;
import dev.will.eventsmanager.events.outbox.OutboxMessage.Type;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;

// Only the Kafka pieces, against a real broker: no database, no web layer.
@SpringBootTest(classes = {KafkaConfig.class, KafkaRegistrationEventPublisher.class})
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
class KafkaRegistrationEventPublisherIntegrationTest {

    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.1");

    static {
        kafka.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.admin.auto-create", () -> "true");
    }

    private static final String PAYLOAD = "{\"eventId\":7,\"userSub\":\"ana\"}";

    @Autowired
    KafkaRegistrationEventPublisher publisher;

    @Test
    void publish_sendsThePayloadKeyedByTheEvent() {
        publisher.publish(OutboxMessage.pending(UUID.randomUUID(), Type.REGISTRATION_CREATED, "7", PAYLOAD));

        ConsumerRecord<String, String> record = readOne();
        then(record.key()).isEqualTo("7");
        then(record.value()).isEqualTo(PAYLOAD);
    }

    // A plain consumer reading the topic from the start: the same thing a consumer service does.
    private ConsumerRecord<String, String> readOne() {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "publisher-test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(KafkaRegistrationEventPublisher.TOPIC));
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
            throw new AssertionError("No message on " + KafkaRegistrationEventPublisher.TOPIC + " within 10 s");
        }
    }
}
