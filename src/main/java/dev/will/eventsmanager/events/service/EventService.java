package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.config.CacheConfig;
import dev.will.eventsmanager.common.exception.ProfileWithoutCoordinatesException;
import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.common.exception.CapacityBelowRegistrationsException;
import dev.will.eventsmanager.common.exception.EventHasActiveRegistrationsException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.dto.response.EventAttendeeResponse;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.mapper.EventRegistrationMapper;
import dev.will.eventsmanager.events.outbox.RegistrationOutbox;
import dev.will.eventsmanager.events.repository.EventRegistrationCount;
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.events.repository.EventSpecs;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class EventService {

    private final EventMapper eventMapper;
    private final EventRegistrationMapper eventRegistrationMapper;
    private final EventRepository eventRepository;
    private final EventRegistrationsRepository eventRegistrationsRepository;
    private final UserRepository userRepository;
    private final RegistrationOutbox registrationOutbox;

    // The key relies on EventFilter always carrying a real `from`; without it a listing cached today would be served tomorrow.
    // The distance depends on who asks, so that listing is never cached.
    @Cacheable(cacheNames = CacheConfig.EVENTS_LISTING, key = "#pageable.toString() + '|' + #filter.toString()",
            condition = "#filter.maxDistanceKm() == null")
    public PageResponse<EventResponse> findAllEvents(Pageable pageable, EventFilter filter, String externalId) {
        Coordinates origin = filter.maxDistanceKm() == null ? null : profileCoordinates(externalId);
        Specification<Event> specification = EventSpecs.from(filter);
        if (origin != null) {
            specification = specification.and(EventSpecs.withinDistanceOf(origin, filter.maxDistanceKm()));
        }
        Page<Event> events = eventRepository.findAll(specification, pageable);
        Map<Long, Long> activeRegistrations = countActiveRegistrations(events.getContent());
        return PageResponse.from(events.map(event -> toListingResponse(
                event, event.remainingSpots(activeRegistrations.getOrDefault(event.getId(), 0L)), origin)));
    }

    private Coordinates profileCoordinates(String externalId) {
        User profile = userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", externalId));
        Coordinates coordinates = profile.getAddress().coordinates();
        if (coordinates == null) {
            throw new ProfileWithoutCoordinatesException();
        }
        return coordinates;
    }

    private EventResponse toListingResponse(Event event, int remainingSpots, Coordinates origin) {
        if (origin == null) {
            return eventMapper.toResponse(event, remainingSpots);
        }
        double distanceKm = origin.distanceKmTo(event.getAddress().coordinates());
        return eventMapper.toResponse(event, remainingSpots, Math.round(distanceKm * 10) / 10.0);
    }

    // One aggregated query for the whole page. Events without active registrations are absent from the map.
    private Map<Long, Long> countActiveRegistrations(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        return eventRegistrationsRepository.countByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventRegistrationCount::eventId, EventRegistrationCount::total));
    }

    public EventResponse findEventById(Long id) throws ResourceNotFoundException {
        Event event = eventRepository.findByIdWithAddress(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        long activeRegistrations = eventRegistrationsRepository.countByEventId(id);
        return eventMapper.toResponse(event, event.remainingSpots(activeRegistrations));
    }

    // allEntries: a listing key cannot be derived from one event, so any change drops every cached page.
    @CacheEvict(cacheNames = CacheConfig.EVENTS_LISTING, allEntries = true)
    public EventResponse createEvent(EventCreateRequest eventCreateRequest, Coordinates coordinates) {
        Event entity = eventMapper.toEntity(eventCreateRequest);
        entity.getAddress().locateAt(coordinates);
        Event saved = eventRepository.save(entity);
        return eventMapper.toResponse(saved, saved.remainingSpots(0));
    }

    @CacheEvict(cacheNames = CacheConfig.EVENTS_LISTING, allEntries = true)
    public EventResponse updateEvent(Long eventId, EventCreateRequest request, Coordinates coordinates) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        long activeRegistrations = eventRegistrationsRepository.countByEventId(eventId);
        if (request.capacity() < activeRegistrations) {
            throw new CapacityBelowRegistrationsException(activeRegistrations);
        }

        List<String> geocodedFieldsBefore = event.getAddress().geocodedFields();
        eventMapper.updateEntity(request, event);
        event.getAddress().relocate(coordinates, geocodedFieldsBefore);
        return eventMapper.toResponse(event, event.remainingSpots(activeRegistrations));
    }

    @CacheEvict(cacheNames = CacheConfig.EVENTS_LISTING, allEntries = true)
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        long activeRegistrations = eventRegistrationsRepository.countByEventId(eventId);
        if (activeRegistrations > 0) {
            throw new EventHasActiveRegistrationsException(activeRegistrations);
        }

        eventRepository.delete(event);
    }

    public PageResponse<EventAttendeeResponse> findEventAttendees(Long eventId, Pageable pageable) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", eventId);
        }
        Page<EventRegistration> registrations = eventRegistrationsRepository.findByEventIdWithUser(eventId, pageable);
        return PageResponse.from(registrations.map(eventRegistrationMapper::toAttendee));
    }

    public EventRegistrationResponse registerUserForEvent(String externalId, Long eventId) {

        User user = userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("User", externalId));

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        Optional<EventRegistration> existingOpt = eventRegistrationsRepository
                .findByUserIdAndEventIdIncludeDeleted(user.getId(), event.getId());

        if (existingOpt.isPresent()) {
            EventRegistration registration = existingOpt.get();
            if (registration.isDeleted()) {
                ensureEventHasRoom(event);
            }
            registration.restore();
            EventRegistration restored = eventRegistrationsRepository.save(registration);
            registrationOutbox.recordCreated(event.getId(), externalId);
            return eventRegistrationMapper.toResponse(restored);
        }

        ensureEventHasRoom(event);

        EventRegistration newRegistration = new EventRegistration();
        newRegistration.setUser(user);
        newRegistration.setEvent(event);

        EventRegistration saved = eventRegistrationsRepository.save(newRegistration);
        registrationOutbox.recordCreated(event.getId(), externalId);
        return eventRegistrationMapper.toResponse(saved);
    }

    private void ensureEventHasRoom(Event event) {
        event.ensureHasRoom(eventRegistrationsRepository.countByEventId(event.getId()));
    }

    public EventRegistrationResponse findUserRegistration(Long eventId, String externalId) {
        User user = userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("User", externalId));

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", eventId);
        }

        EventRegistration registration = eventRegistrationsRepository
                .findByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", eventId));

        return eventRegistrationMapper.toResponse(registration);
    }

    public void unregisterUserFromEvent(Long eventId, String externalId) {
        User user = userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("User", externalId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        EventRegistration registration = eventRegistrationsRepository
                .findByUserIdAndEventId(user.getId(), event.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registration", eventId));

        eventRegistrationsRepository.delete(registration);
        registrationOutbox.recordCancelled(event.getId(), externalId);
    }
}
