package dev.will.eventsmanager.events.outbox;

import dev.will.eventsmanager.SharedKafka;
import dev.will.eventsmanager.common.config.KafkaConfig;
import dev.will.eventsmanager.events.outbox.OutboxMessage.Type;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;

// Only the Kafka pieces, against a real broker: no database, no web layer.
@SpringBootTest(classes = {KafkaConfig.class, KafkaRegistrationEventPublisher.class})
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
class KafkaRegistrationEventPublisherIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        SharedKafka.register(registry);
    }

    private static final String KEY = "publisher-test";
    private static final String PAYLOAD = "{\"eventId\":7,\"userSub\":\"ana\"}";

    @Autowired
    KafkaRegistrationEventPublisher publisher;

    @Test
    void publish_sendsThePayloadUnderTheMessageKey() {
        publisher.publish(OutboxMessage.pending(UUID.randomUUID(), Type.REGISTRATION_CREATED, KEY, PAYLOAD));

        ConsumerRecord<String, String> consumerRecord = SharedKafka.readByKey(KafkaRegistrationEventPublisher.TOPIC, KEY);
        then(consumerRecord.value()).isEqualTo(PAYLOAD);
    }
}
