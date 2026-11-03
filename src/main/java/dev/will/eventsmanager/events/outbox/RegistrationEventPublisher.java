package dev.will.eventsmanager.events.outbox;

// Keeps the broker out of the outbox logic: the relay only knows this interface, Kafka is one implementation.
public interface RegistrationEventPublisher {

    // Returns only once the broker has confirmed the message; throws if it could not, so the row stays pending.
    void publish(OutboxMessage message);
}
