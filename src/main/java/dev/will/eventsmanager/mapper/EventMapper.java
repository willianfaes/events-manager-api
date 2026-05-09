package dev.will.eventsmanager.mapper;

import dev.will.eventsmanager.dto.request.EventCreateRequest;
import dev.will.eventsmanager.dto.response.EventResponse;
import dev.will.eventsmanager.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EventAddressMapper.class}) // Makes MapStruct generate a Spring component
public interface EventMapper {

    @Mapping(target = "id", ignore = true) // ID is auto-generated
    Event toEntity(EventCreateRequest request);

    EventResponse toResponse(Event event);
}