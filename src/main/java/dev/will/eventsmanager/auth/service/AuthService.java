package dev.will.eventsmanager.auth.service;

import dev.will.eventsmanager.auth.dto.request.LoginRequest;
import dev.will.eventsmanager.auth.dto.request.RegisterUserRequest;
import dev.will.eventsmanager.auth.dto.response.LoginResponse;
import dev.will.eventsmanager.auth.dto.response.RegisterUserResponse;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.auth.mapper.LoginMapper;
import dev.will.eventsmanager.auth.mapper.RegisterUserMapper;
import dev.will.eventsmanager.users.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final LoginMapper loginMapper;
    private final RegisterUserMapper registerUserMapper;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService,
                       PasswordEncoder passwordEncoder, UserRepository userRepository,
                       LoginMapper loginMapper, RegisterUserMapper registerUserMapper) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.loginMapper = loginMapper;
        this.registerUserMapper = registerUserMapper;
    }

    public LoginResponse login(LoginRequest loginRequest) {
    UsernamePasswordAuthenticationToken userAndPass =
            new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());

    Authentication authentication = authenticationManager.authenticate(userAndPass);
    User user = (User) authentication.getPrincipal();

    String token = tokenService.generateToken(user);

    return loginMapper.toLoginResponse(user, token);
    }

    public RegisterUserResponse register(RegisterUserRequest request) {
        User newUser = registerUserMapper.toEntity(request);

        newUser.setPassword(passwordEncoder.encode(request.password()));
        // Encodes password with BCrypt, then saves it inside newUser variable.

        User savedUser = userRepository.save(newUser);
        // This catches the whole user line in the DB, right after the ID is generated,
        // so savedUser now has even the ID! Full line!

        return registerUserMapper.toResponse(savedUser);
    }
}