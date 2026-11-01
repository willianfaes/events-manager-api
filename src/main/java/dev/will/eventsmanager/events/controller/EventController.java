package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.docs.Authenticated;
import dev.will.eventsmanager.common.exception.ErrorResponse;
import dev.will.eventsmanager.common.pagination.EventSort;
import dev.will.eventsmanager.common.pagination.PageRequests;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/events")
@Authenticated
@Tag(name = "Events", description = "Events and their registrations")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Get all events with pagination")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved events. "
            + "The listing is cached for 30 seconds and cleared when an event is created, updated or deleted. "
            + "remainingSpots is a snapshot and may be up to 30 seconds stale; registering is the source of truth "
            + "and answers 409 when the event is full. "
            + "With maxDistanceKm the listing depends on the profile, is not cached and adds distanceKm to each event.")
    @ApiResponse(responseCode = "400", description = "maxDistanceKm is not a positive number",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "maxDistanceKm was sent, but the user has no profile",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "maxDistanceKm was sent, but the address of the profile could not be located",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PageResponse<EventResponse>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DATE_ASC") EventSort sort,
            @Valid @ParameterObject EventFilter filter,
            Principal principal) {
        Pageable pageable = PageRequests.of(page, size, sort.toSort());
        PageResponse<EventResponse> events = eventService.findAllEvents(pageable, filter, principal.getName());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Returns a single event, including its address")
    @ApiResponse(responseCode = "200", description = "Event found. "
            + "remainingSpots is a snapshot and may be stale; registering is the source of truth "
            + "and answers 409 when the event is full.")
    @ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.findEventById(id);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/{eventId}/registrations")
    @Operation(
            summary = "Register for an event",
            description = "Registers the authenticated user for the specified event.")
    @ApiResponse(responseCode = "201", description =
            "The user has been registered; Location header points to GET /events/{eventId}/registrations/me. "
                    + "A previously cancelled registration is revived.")
    @ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409",
            description = "The user is already registered for this event, or the event is full",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EventRegistrationResponse> registerForEvent(
            @PathVariable Long eventId,
            Principal principal
    ) {
        EventRegistrationResponse registration = eventService.registerUserForEvent(principal.getName(), eventId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/me")
                .build()
                .toUri();

        return ResponseEntity.created(location).body(registration);
    }

    @GetMapping("/{eventId}/registrations/me")
    @Operation(
            summary = "Get my registration for an event",
            description = "Returns the authenticated user's active registration for the specified event.")
    @ApiResponse(responseCode = "200", description = "The user is registered for the event")
    @ApiResponse(responseCode = "404",
            description = "Event not found, or the user has no active registration for it (including a cancelled one)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EventRegistrationResponse> getMyRegistration(
            @PathVariable Long eventId,
            Principal principal
    ) {
        return ResponseEntity.ok(eventService.findUserRegistration(eventId, principal.getName()));
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
