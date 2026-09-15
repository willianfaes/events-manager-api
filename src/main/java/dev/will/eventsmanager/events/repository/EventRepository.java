package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e JOIN FETCH e.address")
    Page<Event> findAllWithAddress(Pageable pageable);
}