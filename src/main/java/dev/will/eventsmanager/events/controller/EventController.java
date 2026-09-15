package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/events")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventController(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    @GetMapping
    @Operation(summary = "Get all events with pagination")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved events")
    public ResponseEntity<PageResponse<EventResponse>> getAllEvents(
            @ParameterObject
            Pageable pageable) {
        PageResponse<EventResponse> events = eventService.findAllEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return eventService.findEventById(id)
                .map(eventMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request));
    }

    @PostMapping("/{eventId}/registrations")
    public ResponseEntity<EventRegistrationResponse> registerForEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails loggedUser
    ) {
        // 1. Pegamos o e-mail que está guardado dentro do Token
        String userEmail = loggedUser.getUsername();

        // 2. Passamos o E-MAIL (String) e o Event ID (Long) para a Service
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.registerUserForEvent(userEmail, eventId));
    }

    @DeleteMapping("/{eventId}/registrations")
    public ResponseEntity<Void> unregisterFromEvent(@PathVariable Long eventId, Principal principal) {
        eventService.unregisterUserFromEvent(eventId, principal.getName());
        // Assuming principal.getName() is the user's email
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
