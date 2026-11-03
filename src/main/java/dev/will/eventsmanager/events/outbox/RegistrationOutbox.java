package dev.will.eventsmanager.events.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.will.eventsmanager.events.outbox.OutboxMessage.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// Writes registration messages to the outbox. Called inside the registration transaction, so a message exists
// only if the change commits. The event id is the broker key: messages of the same event keep their order.
@Component
@RequiredArgsConstructor
public class RegistrationOutbox {

    private final OutboxMessageRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void recordCreated(Long eventId, String userSub) {
        record(Type.REGISTRATION_CREATED, eventId, userSub);
    }

    public void recordCancelled(Long eventId, String userSub) {
        record(Type.REGISTRATION_CANCELLED, eventId, userSub);
    }

    private void record(Type type, Long eventId, String userSub) {
        UUID messageId = UUID.randomUUID();
        RegistrationMessage message = new RegistrationMessage(messageId, type, Instant.now(), eventId, userSub);
        outboxRepository.save(OutboxMessage.pending(messageId, type, eventId.toString(), toJson(message)));
    }

    private String toJson(RegistrationMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the registration message", e);
        }
    }

    // The contract with the consumers: the payload of every registration message.
    record RegistrationMessage(UUID messageId, Type type, Instant occurredAt, Long eventId, String userSub) {
    }
}
