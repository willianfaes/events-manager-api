package dev.will.eventsmanager.users.repository;

import dev.will.eventsmanager.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Essencial para login e para buscar o usuário na hora da inscrição
    Optional<User> findByEmail(String email);

    Optional<User> findByExternalId(String externalId);
}