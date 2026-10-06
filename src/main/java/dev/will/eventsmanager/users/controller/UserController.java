package dev.will.eventsmanager.users.controller;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.pagination.PageRequests;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import dev.will.eventsmanager.common.pagination.RegistrationSort;
import dev.will.eventsmanager.users.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me/registrations")
    public ResponseEntity<PageResponse<MyRegistrationResponse>> getUserRegisteredEvents(
            @AuthenticationPrincipal String loggedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "NEWEST") RegistrationSort sort
    ) {
        Pageable pageable = PageRequests.of(page, size, sort.toSort());
        PageResponse<MyRegistrationResponse> registeredEvents =
                userService.getUserRegisteredEvents(loggedUser, pageable);
        return ResponseEntity.ok(registeredEvents);
    }
}
