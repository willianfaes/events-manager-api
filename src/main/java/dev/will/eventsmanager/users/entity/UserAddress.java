package dev.will.eventsmanager.users.entity;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Arrays;
import java.util.List;

@Entity
@SQLDelete(sql = "UPDATE user_addresses SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_addresses")
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String street;

    @Column(length = 100)
    private String complement;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String stateOrProvince;

    @Column(nullable = false, length = 12)
    private String postalCode;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public void locateAt(Coordinates coordinates) {
        this.latitude = coordinates == null ? null : coordinates.latitude();
        this.longitude = coordinates == null ? null : coordinates.longitude();
    }

    // The fields the geocoding lookup reads: the street is never sent, so it does not move the point.
    public List<String> geocodedFields() {
        return Arrays.asList(postalCode, city, countryCode);
    }

    // A failed lookup (null) keeps the old point when the geocoded fields did not change: it is still the right area.
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