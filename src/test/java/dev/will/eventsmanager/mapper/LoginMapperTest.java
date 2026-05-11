package dev.will.eventsmanager.mapper;

import dev.will.eventsmanager.auth.dto.response.LoginResponse;
import dev.will.eventsmanager.auth.mapper.LoginMapper;
import dev.will.eventsmanager.users.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoginMapperTest {

    private LoginMapper mapper = Mappers.getMapper(LoginMapper.class);

    @Test
    void toLoginResponse_shouldMapUserAndTokenCorrectly() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("password"); // Not mapped, but good to have a complete object

        String token = "test-jwt-token";

        // When
        LoginResponse loginResponse = mapper.toLoginResponse(user, token);

        // Then
        assertNotNull(loginResponse);
        assertEquals(user.getId(), loginResponse.userId());
        assertEquals(token, loginResponse.token());
    }
}
