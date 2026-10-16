package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.entity.Event;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class EventSpecs {

    private EventSpecs() {}

    public static Specification<Event> from(EventFilter filter) {
        return Specification.allOf(
                scheduledFrom(filter.from()),
                nameContains(filter.name()),
                scheduledUntil(filter.to()));
    }

    private static Specification<Event> scheduledFrom(LocalDate date) {
        return (event, query, cb) ->
                cb.greaterThanOrEqualTo(event.get("schedule"), date.atStartOfDay());
    }

    private static Specification<Event> scheduledUntil(LocalDate date) {
        if (date == null) return null;
        return (event, query, cb) ->
                cb.lessThan(event.get("schedule"), date.plusDays(1).atStartOfDay());
    }

    private static Specification<Event> nameContains(String name) {
        if (name == null || name.isBlank()) return null;
        return (event, query, cb) ->
                cb.like(cb.lower(event.get("name")), "%" + name.toLowerCase() + "%");
    }
}
