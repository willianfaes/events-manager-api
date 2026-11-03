package dev.will.eventsmanager.events.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class KafkaRegistrationEventPublisher implements RegistrationEventPublisher {

    public static final String TOPIC = "registration-events";

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;

    // The key is the event id, so every message of one event lands on the same partition, in order.
    // send() is asynchronous: waiting on the result is what tells us the broker has the message.
    @Override
    public void publish(OutboxMessage message) {
        try {
            kafkaTemplate.send(TOPIC, message.getMessageKey(), message.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing message " + message.getMessageId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish message " + message.getMessageId(), e);
        }
    }
}
