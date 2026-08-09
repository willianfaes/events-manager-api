package dev.will.eventsmanager.events.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "event_addresses")
// 1. Override the delete command:
@SQLDelete(sql = "UPDATE event_addresses SET is_deleted = true WHERE id=?")
// 2. Filter all future queries:
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 20) // Number can be alphanumeric
    private String number;

    @Column(length = 100) // Complement is optional
    private String complement;

    @Column(nullable = false, length = 100)
    private String neighborhood;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false,length = 20) // Postal code format can vary
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String country;
}