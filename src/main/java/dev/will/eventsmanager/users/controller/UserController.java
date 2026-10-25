package dev.will.eventsmanager.users.controller;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.docs.Authenticated;
import dev.will.eventsmanager.common.exception.ErrorResponse;
import dev.will.eventsmanager.common.pagination.PageRequests;
import dev.will.eventsmanager.common.pagination.RegistrationSort;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import dev.will.eventsmanager.users.dto.response.RegisteredEventsResponse;
import dev.will.eventsmanager.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Set;


@RestController
@RequestMapping("/users")
@Authenticated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me/registrations")
    @Operation(summary = "Get user registered events")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user registered events")
    public ResponseEntity<PageResponse<MyRegistrationResponse>> getUserRegisteredEvents(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "NEWEST") RegistrationSort sort
    ) {
        Pageable pageable = PageRequests.of(page, size, sort.toSort());
        PageResponse<MyRegistrationResponse> registeredEvents =
                userService.getUserRegisteredEvents(principal.getName(), pageable);
        return ResponseEntity.ok(registeredEvents);
    }

    @GetMapping("/me/registrations/events")
    @Operation(
            summary = "Check which of the given events the user is registered for",
            description = "Returns, from the given event ids, the ones the authenticated user has an active "
                    + "registration for. Meant for list screens that show register or cancel. "
                    + "Ids that match no event or a cancelled registration are simply left out.")
    @ApiResponse(responseCode = "200", description = "The ids the user is registered for (possibly empty)")
    @ApiResponse(responseCode = "400",
            description = "eventIds is missing, empty, has a non-numeric value or has more than 50 ids",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RegisteredEventsResponse> getRegisteredEventIds(
            Principal principal,
            @RequestParam @NotEmpty @Size(max = PageRequests.MAX_SIZE) Set<Long> eventIds
    ) {
        return ResponseEntity.ok(userService.findRegisteredEventIds(principal.getName(), eventIds));
    }
}
