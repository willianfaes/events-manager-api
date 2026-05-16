package dev.will.eventsmanager.users.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
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

    @Column(nullable = false, length = 20)
    private String street;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(length = 20)
    private String complement;

    @Column(nullable = false, length = 20)
    private String neighborhood;

    @Column(nullable = false, length = 20)
    private String city;

    @Column(nullable = false, length = 20)
    private String state;

    @Column(nullable = false, length = 20)
    private String postal_code;

    @Column(nullable = false, length = 20)
    private String country;
}