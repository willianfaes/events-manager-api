package dev.will.eventsmanager.events.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    public enum Type {
        REGISTRATION_CREATED,
        REGISTRATION_CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, updatable = false, unique = true)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Type type;

    @Column(name = "message_key", nullable = false, updatable = false)
    private String messageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    private OutboxMessage(UUID messageId, Type type, String messageKey, String payload) {
        this.messageId = messageId;
        this.type = type;
        this.messageKey = messageKey;
        this.payload = payload;
    }

    public static OutboxMessage pending(UUID messageId, Type type, String messageKey, String payload) {
        return new OutboxMessage(messageId, type, messageKey, payload);
    }

    public boolean isSent() {
        return sentAt != null;
    }

    public void markSent(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
