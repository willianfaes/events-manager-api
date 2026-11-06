package dev.will.eventsmanager.events.entity;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Arrays;
import java.util.List;

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

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // Null clears them: the address may have changed, and old coordinates would point to the wrong place.
    public void locateAt(Coordinates coordinates) {
        this.latitude = coordinates == null ? null : coordinates.latitude();
        this.longitude = coordinates == null ? null : coordinates.longitude();
    }

    // The fields the geocoding lookup reads; complement, venue and state do not move the point.
    public List<String> geocodedFields() {
        return Arrays.asList(street, postalCode, city, countryCode);
    }

    // A failed lookup (null) keeps the old point when the geocoded fields did not change: it is still the right place.
    public void relocate(Coordinates coordinates, List<String> fieldsBefore) {
        if (coordinates == null && fieldsBefore.equals(geocodedFields())) {
            return;
        }
        locateAt(coordinates);
    }

    public Coordinates coordinates() {
        return latitude == null ? null : new Coordinates(latitude, longitude);
    }
}