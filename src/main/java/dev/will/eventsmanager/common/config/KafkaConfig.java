package dev.will.eventsmanager.common.config;

import dev.will.eventsmanager.events.outbox.KafkaRegistrationEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Created on startup if missing. Three partitions let up to three consumers of one group read in parallel;
    // one replica because there is a single broker (so it is not fault tolerant).
    @Bean
    public NewTopic registrationEventsTopic() {
        return TopicBuilder.name(KafkaRegistrationEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
