package dev.will.eventsmanager.events.mapper;

import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.response.EventAddressResponse ;
import dev.will.eventsmanager.events.entity.EventAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface EventAddressMapper {

    @Mapping(target = "id", ignore = true)
    EventAddress toEntity(EventAddressRequest request);
    EventAddressResponse toResponse(EventAddress address);
}
