package dev.will.eventsmanager.events.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "event_addresses")
@SQLDelete(sql = "UPDATE event_addresses SET is_deleted = true WHERE id=?")
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

    @Column(nullable = false, length = 150)
    private String street;

    @Column(length = 100)
    private String complement;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "state_or_province", length = 100)
    private String stateOrProvince;

    @Column(name = "postal_code", nullable = false, length = 12)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "venue_name", length = 255)
    private String venueName;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}