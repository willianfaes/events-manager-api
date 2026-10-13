package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.entity.EventRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface EventRegistrationsRepository extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByUserIdAndEventId(Long userId, Long eventId);

    long countByEventId(Long eventId);

    // Events without active registrations are absent from the result. @SQLRestriction already excludes cancelled ones.
    @Query("SELECT new dev.will.eventsmanager.events.repository.EventRegistrationCount(r.event.id, COUNT(r)) "
            + "FROM EventRegistration r WHERE r.event.id IN :eventIds GROUP BY r.event.id")
    List<EventRegistrationCount> countByEventIds(@Param("eventIds") Collection<Long> eventIds);

    // Native query bypasses @SQLRestriction to find soft-deleted registrations.
    @Query(value = "SELECT * FROM event_registrations WHERE user_id = :userId AND event_id = :eventId",
            nativeQuery = true)
    Optional<EventRegistration> findByUserIdAndEventIdIncludeDeleted(@Param("userId") Long userId,
                                                                     @Param("eventId") Long eventId);

    @Query(value = "SELECT r FROM EventRegistration r JOIN FETCH r.event e JOIN FETCH e.address WHERE r.user.email = :email",
            countQuery = "SELECT COUNT(r) FROM EventRegistration r WHERE r.user.email = :email")
    Page<EventRegistration> findByUserEmailWithEvent(@Param("email") String email, Pageable pageable);

    @Query("SELECT r.event.id FROM EventRegistration r "
            + "WHERE r.user.email = :email AND r.event.id IN :eventIds ORDER BY r.event.id")
    List<Long> findRegisteredEventIds(@Param("email") String email, @Param("eventIds") Collection<Long> eventIds);

    @Query(value = "SELECT r FROM EventRegistration r JOIN FETCH r.user WHERE r.event.id = :eventId",
            countQuery = "SELECT COUNT(r) FROM EventRegistration r WHERE r.event.id = :eventId")
    Page<EventRegistration> findByEventIdWithUser(@Param("eventId") Long eventId, Pageable pageable);
}