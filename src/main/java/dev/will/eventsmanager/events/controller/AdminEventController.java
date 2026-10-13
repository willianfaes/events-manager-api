package dev.will.eventsmanager.events.controller;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.docs.AdminOnly;
import dev.will.eventsmanager.common.docs.Authenticated;
import dev.will.eventsmanager.common.exception.ErrorResponse;
import dev.will.eventsmanager.common.pagination.AttendeeSort;
import dev.will.eventsmanager.common.pagination.PageRequests;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventAttendeeResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/admin/events")
@PreAuthorize("hasRole('ADMIN')")
@Authenticated
@Tag(name = "Admin", description = "Routes restricted to ROLE_ADMIN")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "Create an event", description = "Creates a new event. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Event created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Invalid payload (e.g. missing category, non-positive capacity, schedule in the past)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse created = eventService.createEvent(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/events/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{eventId}/registrations")
    @AdminOnly
    @Operation(
            summary = "List the attendees of an event",
            description = "Returns the active registrations of the event, paginated. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Page of the event's attendees")
    @ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PageResponse<EventAttendeeResponse>> getEventAttendees(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "NEWEST") AttendeeSort sort) {
        Pageable pageable = PageRequests.of(page, size, sort.toSort());
        return ResponseEntity.ok(eventService.findEventAttendees(eventId, pageable));
    }
}
