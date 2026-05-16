package dev.will.eventsmanager.events.mapper;

import dev.will.eventsmanager.dto.request.AddressRequest;
import dev.will.eventsmanager.dto.response.AddressResponse;
import dev.will.eventsmanager.events.entity.EventAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface EventAddressMapper {

    @Mapping(target = "id", ignore = true)
    EventAddress toEntity(AddressRequest request);
    AddressResponse toResponse(EventAddress address);
}
