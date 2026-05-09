package dev.will.eventsmanager.mapper;

import dev.will.eventsmanager.dto.response.LoginResponse;
import dev.will.eventsmanager.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface LoginMapper {

    /**
     * Maps a User entity to a LoginResponse DTO.
     */

    @Mapping(source = "user.id", target = "userId")
    LoginResponse toLoginResponse(User user, String token);

    // You might also add a method to map LoginRequest to User if needed,
    // but typically LoginRequest is consumed directly by the authentication process.
}