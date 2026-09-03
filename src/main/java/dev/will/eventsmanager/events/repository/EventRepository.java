package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e JOIN FETCH e.address")
    List<Event> findAllWithAddress();
}