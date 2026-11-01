package dev.will.eventsmanager.events.mapper;

import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = EventAddressMapper.class)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    Event toEntity(EventCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registrations", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(EventCreateRequest request, @MappingTarget Event event);

    @Mapping(target = "remainingSpots", source = "remainingSpots")
    @Mapping(target = "distanceKm", ignore = true)
    EventResponse toResponse(Event event, int remainingSpots);

    @Mapping(target = "remainingSpots", source = "remainingSpots")
    @Mapping(target = "distanceKm", source = "distanceKm")
    EventResponse toResponse(Event event, int remainingSpots, Double distanceKm);
}