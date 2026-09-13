package dev.will.eventsmanager.users.mapper;

import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import dev.will.eventsmanager.users.entity.User;

public interface UserAddressMapper {

    User toEntity(UserAddressRequest request);
}
