package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.config.CacheConfig;
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

    // The key relies on EventFilter always carrying a real `from`; without it a listing cached today would be served tomorrow.
    @Cacheable(cacheNames = CacheConfig.EVENTS_LISTING, key = "#pageable.toString() + '|' + #filter.toString()")
    public PageResponse<EventResponse> findAllEvents(Pageable pageable, EventFilter filter) {
        Specification<Event> specification = EventSpecs.from(filter);
        Page<Event> events = eventRepository.findAll(specification, pageable);
        Map<Long, Long> activeRegistrations = countActiveRegistrations(events.getContent());
        return PageResponse.from(events.map(event -> eventMapper.toResponse(
                event, event.remainingSpots(activeRegistrations.getOrDefault(event.getId(), 0L)))));
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

        eventMapper.updateEntity(request, event);
        event.getAddress().locateAt(coordinates);
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
            return eventRegistrationMapper.toResponse(eventRegistrationsRepository.save(registration));
        }

        ensureEventHasRoom(event);

        EventRegistration newRegistration = new EventRegistration();
        newRegistration.setUser(user);
        newRegistration.setEvent(event);

        return eventRegistrationMapper.toResponse(eventRegistrationsRepository.save(newRegistration));
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
    }
}
