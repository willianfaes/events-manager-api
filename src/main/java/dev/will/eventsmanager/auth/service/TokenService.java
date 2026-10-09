package dev.will.eventsmanager.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.will.eventsmanager.auth.security.JWTUserData;
import dev.will.eventsmanager.users.entity.Role;
import dev.will.eventsmanager.users.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Service
public class TokenService {

    private static final int MIN_SECRET_BYTES = 32;

    private final Algorithm algorithm;

    public TokenService(@Value("${jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "jwt.secret must be set and have at least " + MIN_SECRET_BYTES + " bytes (set JWT_SECRET)");
        }
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(User user){
        return JWT.create()
                .withSubject(user.getEmail())
                .withClaim("role", user.getRole().name())
                .withExpiresAt(Instant.now().plusSeconds(86400))
                .withIssuedAt(Instant.now())
                .sign(algorithm);
    }

    public Optional<JWTUserData> validateToken(String token) {

        try {
            DecodedJWT decode = JWT.require(algorithm)
                    .build().verify(token);

            String role = decode.getClaim("role").asString();
            if (role == null) return Optional.empty();

            return Optional.of(JWTUserData.builder()
                    .userId(decode.getClaim("userId").asLong())
                    .email(decode.getSubject())
                    .role(Role.valueOf(role))
                    .build());
        }
        catch (JWTVerificationException ex) {
            return  Optional.empty();

        }

    }
}
