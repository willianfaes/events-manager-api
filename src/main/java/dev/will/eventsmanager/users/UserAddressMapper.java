package dev.will.eventsmanager.users;

import dev.will.eventsmanager.dto.request.AddressRequest;
import dev.will.eventsmanager.events.entity.Event;

public interface UserAddressMapper {

    Event toEntity(AddressRequest request);
}
