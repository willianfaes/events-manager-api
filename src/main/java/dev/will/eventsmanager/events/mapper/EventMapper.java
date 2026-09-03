package dev.will.eventsmanager.events.mapper;

import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EventAddressMapper.class, EventRegistrationMapper.class})
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    Event toEntity(EventCreateRequest request);

    EventResponse toResponse(Event event);
}