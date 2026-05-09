package dev.will.eventsmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
// 1. Override the delete command:
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false") // Banco, só me traga linhas dessa tabela onde "is_deleted" for false! (se a gente usa soft delete, o preço é ter que filtrar assim pra que as soft-deletadas nao venham junto nas queries)
@Getter
@Setter
@NoArgsConstructor // Generates a no-argument constructor, required by JPA
@Builder // Generates a builder pattern for object creation
@AllArgsConstructor // Generates a constructor with all fields (id, name, email, age)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private Integer age;

    @Column(nullable = false)
    private String password;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL) // Or @ManyToOne, depending on your exact relationship
    @JoinColumn(name = "address_id", nullable = false) // @JoinColumn, not @JoinTable, cause it's an auxiliary table, not a junction one (associative)
    private UserAddress address;

    // One Event has MANY registrations
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<EventRegistration> registrations;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }

}