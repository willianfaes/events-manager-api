package dev.will.eventsmanager.events.mapper;

import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.response.EventAddressResponse ;
import dev.will.eventsmanager.events.entity.EventAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface EventAddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    EventAddress toEntity(EventAddressRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    void updateEntity(EventAddressRequest request, @MappingTarget EventAddress address);

    EventAddressResponse toResponse(EventAddress address);
}
