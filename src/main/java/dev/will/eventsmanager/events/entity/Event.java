package dev.will.eventsmanager.events.entity;

import dev.will.eventsmanager.common.exception.EventFullException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
@SQLDelete(sql = "UPDATE events SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private LocalDateTime schedule;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", nullable = false)
    private EventAddress address;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int capacity;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventRegistration> registrations;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public int remainingSpots(long activeRegistrations) {
        if (activeRegistrations > capacity) {
            throw new IllegalStateException("Event " + id + " has " + activeRegistrations
                    + " active registrations for a capacity of " + capacity);
        }
        return capacity - (int) activeRegistrations;
    }

    public void ensureHasRoom(long activeRegistrations) {
        if (activeRegistrations >= capacity) {
            throw new EventFullException();
        }
    }
}