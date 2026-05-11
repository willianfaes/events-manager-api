package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // O JOIN FETCH força o Hibernate a trazer o endereço na mesma busca,
    // evitando o erro "LazyInitializationException".
    @Query("SELECT e FROM Event e JOIN FETCH e.address WHERE e.id = :id")
    Optional<Event> findByIdWithAddress(@Param("id") Long id);

}