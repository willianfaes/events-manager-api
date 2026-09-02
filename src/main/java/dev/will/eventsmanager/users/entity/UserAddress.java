package dev.will.eventsmanager.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_addresses")
public class UserAddress implements Serializable {

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
}