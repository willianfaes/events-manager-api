package dev.will.eventsmanager.events.repository;

import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.entity.Event;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
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

    // Haversine, the same formula as Coordinates.distanceKmTo. An event without coordinates compares to NULL in SQL,
    // so it is left out.
    public static Specification<Event> withinDistanceOf(Coordinates origin, double maxDistanceKm) {
        double originLatitude = Math.toRadians(origin.latitude());
        double originLongitude = Math.toRadians(origin.longitude());
        return (event, query, cb) -> {
            Expression<Double> latitude = function(cb, "radians", event.get("address").get("latitude"));
            Expression<Double> longitude = function(cb, "radians", event.get("address").get("longitude"));
            Expression<Double> halfLatitudeDelta = cb.prod(cb.diff(latitude, originLatitude), 0.5);
            Expression<Double> halfLongitudeDelta = cb.prod(cb.diff(longitude, originLongitude), 0.5);
            Expression<Double> a = cb.sum(
                    sinSquared(cb, halfLatitudeDelta),
                    cb.prod(cb.prod(function(cb, "cos", latitude), Math.cos(originLatitude)),
                            sinSquared(cb, halfLongitudeDelta)));
            Expression<Double> distanceKm = cb.prod(
                    function(cb, "asin", function(cb, "sqrt", a)), 2 * Coordinates.EARTH_RADIUS_KM);
            return cb.lessThanOrEqualTo(distanceKm, maxDistanceKm);
        };
    }

    private static Expression<Double> sinSquared(CriteriaBuilder cb, Expression<Double> angle) {
        return cb.prod(function(cb, "sin", angle), function(cb, "sin", angle));
    }

    private static Expression<Double> function(CriteriaBuilder cb, String name, Expression<?> argument) {
        return cb.function(name, Double.class, argument);
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
