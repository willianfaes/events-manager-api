package dev.will.eventsmanager.events.outbox;

import dev.will.eventsmanager.events.outbox.OutboxMessage.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxMessageRepository outboxRepository;

    @Mock
    RegistrationEventPublisher publisher;

    @InjectMocks
    OutboxRelay relay;

    @Test
    void publishPending_publishesInOrderAndMarksEachMessageSent() {
        OutboxMessage first = pending();
        OutboxMessage second = pending();
        given(outboxRepository.findBySentAtIsNullOrderByIdAsc(any(Limit.class))).willReturn(List.of(first, second));

        relay.publishPending();

        InOrder order = inOrder(publisher);
        order.verify(publisher).publish(first);
        order.verify(publisher).publish(second);
        then(first.isSent()).isTrue();
        then(second.isSent()).isTrue();
        verify(outboxRepository).save(first);
        verify(outboxRepository).save(second);
    }

    @Test
    void publishPending_whenOneFails_stopsAndLeavesItAndTheRestPending() {
        OutboxMessage failing = pending();
        OutboxMessage next = pending();
        given(outboxRepository.findBySentAtIsNullOrderByIdAsc(any(Limit.class))).willReturn(List.of(failing, next));
        willThrow(new IllegalStateException("broker down")).given(publisher).publish(failing);

        relay.publishPending();

        verify(publisher, never()).publish(next);
        then(failing.isSent()).isFalse();
        then(next.isSent()).isFalse();
        verify(outboxRepository, never()).save(any());
    }

    private static OutboxMessage pending() {
        return OutboxMessage.pending(UUID.randomUUID(), Type.REGISTRATION_CREATED, "7", "{}");
    }
}
