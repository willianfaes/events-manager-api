package dev.will.eventsmanager.events.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// Moves pending outbox rows to the broker. No transaction around the loop: waiting on the broker must not hold a
// database connection, so each row is marked sent on its own right after the broker confirms it.
// A crash between the two leaves the row pending and it is sent again: delivery is at least once.
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;

    private final OutboxMessageRepository outboxRepository;
    private final RegistrationEventPublisher publisher;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<OutboxMessage> pending = outboxRepository.findBySentAtIsNullOrderByIdAsc(Limit.of(BATCH_SIZE));
        for (OutboxMessage message : pending) {
            try {
                publisher.publish(message);
            } catch (RuntimeException e) {
                // Stop here: the next rows may belong to the same event and must not overtake this one.
                log.warn("Outbox message {} not published, retrying on the next run: {}",
                        message.getMessageId(), e.getMessage());
                return;
            }
            message.markSent(LocalDateTime.now());
            outboxRepository.save(message);
        }
    }
}
