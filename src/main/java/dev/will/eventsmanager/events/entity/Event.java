package dev.will.eventsmanager.events.entity;

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
// 1. Override the delete command:
@SQLDelete(sql = "UPDATE events SET is_deleted = true WHERE id=?")
// 2. Filter all future queries:
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor // Generates a no-argument constructor, required by JPA
@AllArgsConstructor // Generates a constructor with all fields
@Builder // Generates a builder pattern for object creation
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING) // Stores the enum name as a String in the database
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private LocalDateTime schedule; // Changed from 'time' to 'schedule' to match SQL

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL) // Or @ManyToOne, depending on your exact relationship
    @JoinColumn(name = "address_id", nullable = false) // @JoinColumn, not @JoinTable, cause it's an auxiliary table, not a junction one (associative)
    private EventAddress address;

    @Column(columnDefinition = "TEXT") // Maps to the TEXT type in MySQL
    private String description;

    // One Event has MANY registrations
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

}

