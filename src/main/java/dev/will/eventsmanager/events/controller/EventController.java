package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventController(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    /**
     * GET /events: Retrieve a list of all events.
     * @return ResponseEntity containing a list of EventResponse DTOs.
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventResponse> events = eventService.findAllEvents().stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    /**
     * GET /events/{id}: Retrieve a single event by its ID.
     * @param id The ID of the event to retrieve.
     * @return ResponseEntity containing the EventResponse DTO.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return eventService.findEventById(id)
                .map(eventMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /events: Create a new event.
     * @param request The EventRequest DTO containing event details.
     * @return ResponseEntity containing the created EventResponse DTO.
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request));
    }

    /**
     * POST /events/{eventId}/registrations: Register the authenticated user for an event.
     * @param eventId The ID of the event to register for.
     * @param loggedUser The authenticated user's principal.
     * @return ResponseEntity indicating success or failure.
     */
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

    /**
     * DELETE /events/{eventId}/registrations : Unregister the authenticated user from an event.
     * @param eventId The ID of the event to unregister from.
     * @param principal The authenticated user's principal.
     * @return ResponseEntity indicating success or failure.
     */
    @DeleteMapping("/{eventId}/registrations")
    public ResponseEntity<Void> unregisterFromEvent(@PathVariable Long eventId, Principal principal) {
        eventService.unregisterUserFromEvent(eventId, principal.getName()); // Assuming principal.getName() is the user's email
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
