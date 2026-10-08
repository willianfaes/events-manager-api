package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Override
    @EntityGraph(attributePaths = "address")
    Page<Event> findAll(Specification<Event> spec, Pageable pageable);

    @Query("SELECT e FROM Event e JOIN FETCH e.address WHERE e.id = :id")
    Optional<Event> findByIdWithAddress(Long id);
}
