package dev.will.eventsmanager.events.outbox;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    // Oldest first, so messages with the same key reach the broker in the order they were written.
    List<OutboxMessage> findBySentAtIsNullOrderByIdAsc(Limit limit);
}
