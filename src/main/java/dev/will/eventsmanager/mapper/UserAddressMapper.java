package dev.will.eventsmanager.mapper;

import dev.will.eventsmanager.dto.request.AddressRequest;
import dev.will.eventsmanager.entity.Event;

public interface UserAddressMapper {

    Event toEntity(AddressRequest request);
}
