package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface EventRegistrationsRepository extends JpaRepository<EventRegistration, Long> {

    @Query(value = "SELECT * FROM event_registrations WHERE user_id = :userId AND event_id = :eventId", nativeQuery =
            true)
    Optional<EventRegistration> findByUserIdAndEventIdIncludeDeleted(@Param("userId") Long userId,
                                                                     @Param("eventId") Long eventId);
}