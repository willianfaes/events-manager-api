package dev.will.eventsmanager.users.mapper;

import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.users.dto.response.EventSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventSummaryMapper {

    @Mapping(target = "title", source = "name")
    @Mapping(target = "date", source = "schedule")
    @Mapping(target = "location", source = "address.venueName")
    EventSummaryResponse toEventSummaryResponse(Event event);
}
