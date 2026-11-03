package dev.will.eventsmanager;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One Kafka broker for the whole test run, like {@link SharedPostgres}. Messages are never rolled back, so tests find
 * theirs by key instead of expecting an empty topic.
 */
public final class SharedKafka {

    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    static {
        KAFKA.start();
    }

    private SharedKafka() {}

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.admin.auto-create", () -> "true");
    }

    // A plain consumer, new group each time, reading the topic from the start: what a consumer service does.
    public static ConsumerRecord<String, String> readByKey(String topic, String key) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> consumerRecord : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(consumerRecord.key())) {
                        return consumerRecord;
                    }
                }
            }
            throw new AssertionError("No message with key " + key + " on " + topic + " within 10 s");
        }
    }
}
