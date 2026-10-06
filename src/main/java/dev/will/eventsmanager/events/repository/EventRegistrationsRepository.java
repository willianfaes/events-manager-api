package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.entity.EventRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface EventRegistrationsRepository extends JpaRepository<EventRegistration, Long> {

    @Query(value = "SELECT e FROM EventRegistration e WHERE e.user.id = :userId AND e.event.id = :eventId")
    Optional<EventRegistration> findByUserIdAndEventIdIncludeDeleted(@Param("userId") Long userId,
                                                                     @Param("eventId") Long eventId);

    @Query(value = "SELECT r FROM EventRegistration r JOIN FETCH r.event e JOIN FETCH e.address WHERE r.user.email = :email",
            countQuery = "SELECT COUNT(r) FROM EventRegistration r WHERE r.user.email = :email")
    Page<EventRegistration> findByUserIdWithEvent(@Param("email") String email, Pageable pageable);
}