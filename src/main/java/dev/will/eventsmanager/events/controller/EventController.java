package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.docs.AdminOnly;
import dev.will.eventsmanager.common.docs.Authenticated;
import dev.will.eventsmanager.common.exception.ErrorResponse;
import dev.will.eventsmanager.common.pagination.EventSort;
import dev.will.eventsmanager.common.pagination.PageRequests;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/events")
@Authenticated
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Get all events with pagination")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved events")
    public ResponseEntity<PageResponse<EventResponse>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DATE_ASC") EventSort sort,
            @ParameterObject EventFilter filter) {
        Pageable pageable = PageRequests.of(page, size, sort.toSort());
        PageResponse<EventResponse> events = eventService.findAllEvents(pageable, filter);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Returns a single event, including its address")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.findEventById(id);
        return ResponseEntity.ok(event);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOnly
    @Operation(summary = "Create an event", description = "Creates a new event. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Event created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Invalid payload (e.g. missing category, schedule in the past)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse created = eventService.createEvent(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{eventId}/registrations")
    @Operation(
            summary = "Register for an event",
            description = "Registers the authenticated user for the specified event.")
    @ApiResponse(responseCode = "201", description =
            "The user has been registered; Location header points to the registration. "
                    + "A previously cancelled registration is revived.")
    @ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The user is already registered for this event",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EventRegistrationResponse> registerForEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal String loggedUser
    ) {
        EventRegistrationResponse registration = eventService.registerUserForEvent(loggedUser, eventId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registration.id())
                .toUri();

        return ResponseEntity.created(location).body(registration);
    }

    @DeleteMapping("/{eventId}/registrations")
    @Operation(
            summary = "Unregister from an event",
            description = "Cancels the authenticated user's active registration for the specified event. "
                    + "The user can register again later.")
    @ApiResponse(responseCode = "204", description = "The registration has been cancelled")
    @ApiResponse(responseCode = "404",
            description = "Event not found, or the user has no active registration for it (including a repeated request)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> unregisterFromEvent(@PathVariable Long eventId, Principal principal) {
        eventService.unregisterUserFromEvent(eventId, principal.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
