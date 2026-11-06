package dev.will.eventsmanager.users.mapper;

import dev.will.eventsmanager.users.dto.request.ProfileRequest;
import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(ProfileRequest request);

    ProfileResponse toResponse(User user);

    // The sub comes from the token, never from the body.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "registrations", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(ProfileRequest request, @MappingTarget User user);

    // Updates the same address row in place; the coordinates are left to UserAddress.relocate.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateAddress(UserAddressRequest request, @MappingTarget UserAddress address);
}
