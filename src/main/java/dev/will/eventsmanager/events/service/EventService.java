package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.PageResponse;
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
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.events.repository.EventSpecs;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional
public class EventService {

    private final EventMapper eventMapper;
    private final EventRegistrationMapper eventRegistrationMapper;
    private final EventRepository eventRepository;
    private final EventRegistrationsRepository eventRegistrationsRepository;
    private final UserRepository userRepository;

    public PageResponse<EventResponse> findAllEvents(Pageable pageable, EventFilter filter) {
        Specification<Event> specification = EventSpecs.from(filter);
        Page<Event> events = eventRepository.findAll(specification, pageable);
        return PageResponse.from(events.map(eventMapper::toResponse));
    }

    public EventResponse findEventById(Long id) throws ResourceNotFoundException {
        Event event = eventRepository.findByIdWithAddress(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        return eventMapper.toResponse(event);
    }

    public EventResponse createEvent(EventCreateRequest eventCreateRequest) {
        Event entity = eventMapper.toEntity(eventCreateRequest);
        Event saved = eventRepository.save(entity);
        return eventMapper.toResponse(saved);
    }

    public PageResponse<EventAttendeeResponse> findEventAttendees(Long eventId, Pageable pageable) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", eventId);
        }
        Page<EventRegistration> registrations = eventRegistrationsRepository.findByEventIdWithUser(eventId, pageable);
        return PageResponse.from(registrations.map(eventRegistrationMapper::toAttendee));
    }

    public EventRegistrationResponse registerUserForEvent(String userEmail, Long eventId) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

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

    public EventRegistrationResponse findUserRegistration(Long eventId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", eventId);
        }

        EventRegistration registration = eventRegistrationsRepository
                .findByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", eventId));

        return eventRegistrationMapper.toResponse(registration);
    }

    public void unregisterUserFromEvent(Long eventId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        EventRegistration registration = eventRegistrationsRepository
                .findByUserIdAndEventId(user.getId(), event.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registration", eventId));

        eventRegistrationsRepository.delete(registration);
    }
}
