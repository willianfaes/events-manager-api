package dev.will.eventsmanager.users.mapper;

import dev.will.eventsmanager.users.dto.request.CreateProfileRequest;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import dev.will.eventsmanager.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(CreateProfileRequest request);

    ProfileResponse toResponse(User user);
}
