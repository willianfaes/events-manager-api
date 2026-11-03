package dev.will.eventsmanager.events.outbox;

import dev.will.eventsmanager.AbstractJpaTest;
import dev.will.eventsmanager.events.outbox.OutboxMessage.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMessageRepositoryJpaTest extends AbstractJpaTest {

    private static final String PAYLOAD = "{\"eventId\":7,\"userSub\":\"ana\"}";

    @Autowired
    OutboxMessageRepository outboxRepository;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void hideRowsCommittedByOtherTests() {
        outboxRepository.deleteAll();
    }

    @Test
    void save_shouldStoreAPendingMessageWithItsJsonPayload() throws Exception {
        UUID messageId = UUID.randomUUID();
        Long id = outboxRepository.saveAndFlush(
                OutboxMessage.pending(messageId, Type.REGISTRATION_CREATED, "7", PAYLOAD)).getId();
        em.clear();

        OutboxMessage stored = outboxRepository.findById(id).orElseThrow();

        assertThat(stored.getMessageId()).isEqualTo(messageId);
        assertThat(stored.getType()).isEqualTo(Type.REGISTRATION_CREATED);
        assertThat(stored.getMessageKey()).isEqualTo("7");
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.isSent()).isFalse();
        // JSONB normalizes whitespace, so compare the content, not the text.
        JSONAssert.assertEquals(PAYLOAD, stored.getPayload(), true);
    }

    @Test
    void save_shouldRejectAPayloadThatIsNotJson() {
        OutboxMessage broken = OutboxMessage.pending(UUID.randomUUID(), Type.REGISTRATION_CREATED, "7", "{not json");

        assertThatThrownBy(() -> outboxRepository.saveAndFlush(broken))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findPending_shouldReturnOnlyUnsentMessagesOldestFirstUpToTheLimit() {
        OutboxMessage first = savePending("7");
        OutboxMessage sent = savePending("7");
        sent.markSent(LocalDateTime.now());
        OutboxMessage second = savePending("9");
        OutboxMessage third = savePending("7");
        savePending("4");
        em.flush();
        em.clear();

        List<OutboxMessage> result = outboxRepository.findBySentAtIsNullOrderByIdAsc(Limit.of(3));

        assertThat(result).extracting(OutboxMessage::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    private OutboxMessage savePending(String key) {
        return outboxRepository.save(OutboxMessage.pending(UUID.randomUUID(), Type.REGISTRATION_CREATED, key, PAYLOAD));
    }
}
