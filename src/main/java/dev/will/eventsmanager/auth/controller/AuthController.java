package dev.will.eventsmanager.auth.controller;

import dev.will.eventsmanager.auth.service.AuthService;
import dev.will.eventsmanager.auth.dto.request.LoginRequest;
import dev.will.eventsmanager.auth.dto.request.RegisterUserRequest;
import dev.will.eventsmanager.auth.dto.response.LoginResponse;
import dev.will.eventsmanager.auth.dto.response.RegisterUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }


}
