package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.geocoding.Coordinates;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.exception.AlreadyRegisteredException;
import dev.will.eventsmanager.common.exception.CapacityBelowRegistrationsException;
import dev.will.eventsmanager.common.exception.EventFullException;
import dev.will.eventsmanager.common.exception.EventHasActiveRegistrationsException;
import dev.will.eventsmanager.common.exception.ProfileWithoutCoordinatesException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.dto.response.EventAttendeeResponse;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.mapper.EventRegistrationMapper;
import dev.will.eventsmanager.events.outbox.RegistrationOutbox;
import dev.will.eventsmanager.events.repository.EventRegistrationCount;
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static dev.will.eventsmanager.TestFixtures.USER_SUB;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Coordinates COORDINATES = new Coordinates(45.4390, 10.9942);
    private static final Coordinates MILANO = new Coordinates(45.4642, 9.1900);
    private static final EventFilter WITHIN_200_KM = new EventFilter(null, null, null, 200.0);

    @Mock
    EventRepository eventRepository;

    @Mock
    EventMapper eventMapper;

    @Mock
    EventRegistrationMapper eventRegistrationMapper;

    @Mock
    EventRegistrationsRepository eventRegistrationsRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    RegistrationOutbox registrationOutbox;

    @InjectMocks
    EventService eventService;

    @Test
    void shouldReturnAllEvents() {

        Event fakeEvent = Event.builder().id(1L).capacity(10).build();
        Pageable fakePageable = PageRequest.of(0, 10);
        EventFilter filter = new EventFilter("test", null, null, null);
        Page<Event> fakePage = new PageImpl<>(
                List.of(fakeEvent),
                fakePageable,
                1L
        );
        EventResponse fakeResponse = EventResponse.builder().build();

        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), eq(fakePageable))).thenReturn(fakePage);
        when(eventRegistrationsRepository.countByEventIds(List.of(1L)))
                .thenReturn(List.of(new EventRegistrationCount(1L, 3L)));
        when(eventMapper.toResponse(fakeEvent, 7)).thenReturn(fakeResponse);

        PageResponse<EventResponse> result = eventService.findAllEvents(fakePageable, filter, USER_SUB);

        assertThat(result.content())
                .isNotNull()
                .hasSize(1)
                .containsExactly(fakeResponse);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.last()).isTrue();
    }

    @Test
    void findAllEvents_countsTheRegistrationsOfThePageInOneQuery() {
        Event withRegistrations = Event.builder().id(1L).capacity(10).build();
        Event withoutRegistrations = Event.builder().id(2L).capacity(5).build();
        Pageable pageable = PageRequest.of(0, 10);
        EventResponse first = EventResponse.builder().id(1L).build();
        EventResponse second = EventResponse.builder().id(2L).build();

        given(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(withRegistrations, withoutRegistrations), pageable, 2L));
        given(eventRegistrationsRepository.countByEventIds(List.of(1L, 2L)))
                .willReturn(List.of(new EventRegistrationCount(1L, 3L)));
        given(eventMapper.toResponse(withRegistrations, 7)).willReturn(first);
        given(eventMapper.toResponse(withoutRegistrations, 5)).willReturn(second);

        PageResponse<EventResponse> result = eventService.findAllEvents(pageable, new EventFilter(null, null, null, null), USER_SUB);

        assertThat(result.content()).containsExactly(first, second);
        then(eventRegistrationsRepository).should().countByEventIds(List.of(1L, 2L));
        then(eventRegistrationsRepository).should(never()).countByEventId(anyLong());
    }

    @Test
    void shouldReturnEmptyListWhenNoEventsExist() {

        Pageable fakePageable = PageRequest.of(0, 10);
        Page<Event> fakePage = Page.empty(fakePageable);

        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), eq(fakePageable)))
                .thenReturn(fakePage);

        EventFilter filter = new EventFilter("test", null, null, null);
        PageResponse<EventResponse> result = eventService.findAllEvents(fakePageable, filter, USER_SUB);

        assertThat(result.content()).isEmpty();
        then(eventRegistrationsRepository).shouldHaveNoInteractions();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.last()).isTrue();
    }

    @Test
    void findAllEvents_withMaxDistance_returnsTheDistanceFromTheProfileRoundedToOneDecimal() {
        User profile = validUser();
        profile.getAddress().locateAt(MILANO);
        Event eventInVerona = Event.builder().id(1L).capacity(10)
                .address(EventAddress.builder().latitude(COORDINATES.latitude()).longitude(COORDINATES.longitude()).build())
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        EventResponse expected = EventResponse.builder().build();
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(profile));
        given(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(eventInVerona), pageable, 1));
        // Milano to Verona is 140.76 km by Haversine.
        given(eventMapper.toResponse(eventInVerona, 10, 140.8)).willReturn(expected);

        PageResponse<EventResponse> result = eventService.findAllEvents(pageable, WITHIN_200_KM, USER_SUB);

        assertThat(result.content()).containsExactly(expected);
    }

    @Test
    void findAllEvents_withMaxDistance_whenTheProfileHasNoCoordinates_throwsAndDoesNotQuery() {
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(validUser()));

        assertThatThrownBy(() -> eventService.findAllEvents(PageRequest.of(0, 10), WITHIN_200_KM, USER_SUB))
                .isInstanceOf(ProfileWithoutCoordinatesException.class);
        then(eventRepository).shouldHaveNoInteractions();
    }

    @Test
    void findAllEvents_withMaxDistance_whenThereIsNoProfile_throwsResourceNotFound() {
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findAllEvents(PageRequest.of(0, 10), WITHIN_200_KM, USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class);
        then(eventRepository).shouldHaveNoInteractions();
    }

    @Test
    void findEventById_whenEventExists_returnsMappedResponse() {
        Long id = 1L;
        Event event = Event.builder().id(id).capacity(10).build();
        EventResponse expectedResponse = EventResponse.builder().id(id).build();

        when(eventRepository.findByIdWithAddress(id)).thenReturn(Optional.of(event));
        when(eventRegistrationsRepository.countByEventId(id)).thenReturn(3L);
        when(eventMapper.toResponse(event, 7)).thenReturn(expectedResponse);

        EventResponse result = eventService.findEventById(id);

        assertThat(result).isEqualTo(expectedResponse);
        verify(eventRepository).findByIdWithAddress(id);
        verify(eventMapper).toResponse(event, 7);
    }

    @Test
    void getEventById_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long id = 999L;
        when(eventRepository.findByIdWithAddress(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findEventById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event")
                .hasMessageContaining(id.toString());
    }

    @Test
    void createEvent_returnsResponseMappedFromSavedEntity() {
        EventAddressRequest address = EventAddressRequest.builder()
                .street("Main Street")
                .city("São Paulo")
                .stateOrProvince("SP")
                .postalCode("01310-100")
                .build();

        EventCreateRequest request = EventCreateRequest.builder()
                .name("Tech Conference 2026")
                .category(Category.CULTURAL)
                .schedule(LocalDateTime.now().plusDays(30))
                .address(address)
                .description("Annual tech conference")
                .build();

        Event toPersist = Event.builder().address(EventAddress.builder().build()).build();
        Event saved = Event.builder().id(1L).capacity(20).build();
        EventResponse expected = EventResponse.builder().id(1L).build();

        when(eventMapper.toEntity(request)).thenReturn(toPersist);
        when(eventRepository.save(toPersist)).thenReturn(saved);
        when(eventMapper.toResponse(saved, 20)).thenReturn(expected);

        EventResponse result = eventService.createEvent(request, COORDINATES);

        assertThat(result).isSameAs(expected);
        assertThat(toPersist.getAddress().getLatitude()).isEqualTo(COORDINATES.latitude());
        assertThat(toPersist.getAddress().getLongitude()).isEqualTo(COORDINATES.longitude());
        then(eventRegistrationsRepository).shouldHaveNoInteractions();
    }

    @Test
    void updateEvent_whenCapacityCoversTheActiveRegistrations_updatesTheEvent() {
        Long eventId = 10L;
        Event event = Event.builder().id(eventId).capacity(50).address(EventAddress.builder().build()).build();
        EventCreateRequest request = EventCreateRequest.builder().name("New name").capacity(3).build();
        EventResponse expected = EventResponse.builder().id(eventId).build();

        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(3L);
        // The mapper is a mock, so apply the new capacity the way the real one does.
        willAnswer(invocation -> {
            event.setCapacity(request.capacity());
            return null;
        }).given(eventMapper).updateEntity(request, event);
        given(eventMapper.toResponse(event, 0)).willReturn(expected);

        EventResponse result = eventService.updateEvent(eventId, request, COORDINATES);

        then(eventMapper).should().updateEntity(request, event);
        assertThat(result).isSameAs(expected);
        assertThat(event.getAddress().getLatitude()).isEqualTo(COORDINATES.latitude());
    }

    @Test
    void updateEvent_whenCapacityIsBelowTheActiveRegistrations_throwsAndKeepsTheEvent() {
        Long eventId = 10L;
        Event event = Event.builder().id(eventId).capacity(50).build();
        EventCreateRequest request = EventCreateRequest.builder().capacity(2).build();

        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(3L);

        assertThatThrownBy(() -> eventService.updateEvent(eventId, request, null))
                .isInstanceOf(CapacityBelowRegistrationsException.class)
                .hasMessageContaining("3");

        then(eventMapper).should(never()).updateEntity(any(), any());
    }

    @Test
    void updateEvent_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long eventId = 99L;
        EventCreateRequest request = EventCreateRequest.builder().capacity(10).build();

        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(eventId, request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event");

        then(eventMapper).should(never()).updateEntity(any(), any());
    }

    @Test
    void deleteEvent_whenNoActiveRegistrations_deletesTheEvent() {
        Long eventId = 10L;
        Event event = Event.builder().id(eventId).build();

        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(0L);

        eventService.deleteEvent(eventId);

        then(eventRepository).should().delete(event);
    }

    @Test
    void deleteEvent_whenThereAreActiveRegistrations_throwsAndKeepsTheEvent() {
        Long eventId = 10L;
        Event event = Event.builder().id(eventId).build();

        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(2L);

        assertThatThrownBy(() -> eventService.deleteEvent(eventId))
                .isInstanceOf(EventHasActiveRegistrationsException.class)
                .hasMessageContaining("2");

        then(eventRepository).should(never()).delete(any(Event.class));
    }

    @Test
    void deleteEvent_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long eventId = 99L;

        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event");

        then(eventRepository).should(never()).delete(any(Event.class));
    }

    @Test
    void unregisterUserFromEvent_whenActiveRegistration_softDeletesIt() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).build();
        EventRegistration registration = EventRegistration.builder().user(user).event(event).build();

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventId(1L, eventId)).willReturn(Optional.of(registration));

        eventService.unregisterUserFromEvent(eventId, USER_SUB);

        then(eventRegistrationsRepository).should().delete(registration);
        then(registrationOutbox).should().recordCancelled(eventId, USER_SUB);
    }

    @Test
    void unregisterUserFromEvent_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long eventId = 99L;
        User user = validUser();
        user.setId(1L);

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.unregisterUserFromEvent(eventId, USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event");

        then(eventRegistrationsRepository).should(never()).delete(any());
    }

    @Test
    void unregisterUserFromEvent_whenNoActiveRegistration_throwsResourceNotFoundException() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).build();

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventId(1L, eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.unregisterUserFromEvent(eventId, USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Registration");

        then(eventRegistrationsRepository).should(never()).delete(any());
    }

    @Test
    void findUserRegistration_whenActiveRegistration_returnsIt() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).build();
        EventRegistration registration = EventRegistration.builder().id(7L).user(user).event(event).build();
        EventRegistrationResponse expected = new EventRegistrationResponse(7L, eventId);

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.existsById(eventId)).willReturn(true);
        given(eventRegistrationsRepository.findByUserIdAndEventId(1L, eventId)).willReturn(Optional.of(registration));
        given(eventRegistrationMapper.toResponse(registration)).willReturn(expected);

        EventRegistrationResponse result = eventService.findUserRegistration(eventId, USER_SUB);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void findUserRegistration_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long eventId = 99L;
        User user = validUser();
        user.setId(1L);

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.existsById(eventId)).willReturn(false);

        assertThatThrownBy(() -> eventService.findUserRegistration(eventId, USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event");
    }

    @Test
    void findUserRegistration_whenNoActiveRegistration_throwsResourceNotFoundException() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.existsById(eventId)).willReturn(true);
        given(eventRegistrationsRepository.findByUserIdAndEventId(1L, eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findUserRegistration(eventId, USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Registration");
    }

    @Test
    void findEventAttendees_whenEventExists_returnsMappedPage() {
        Long eventId = 10L;
        Pageable pageable = PageRequest.of(0, 20);
        EventRegistration registration = EventRegistration.builder().id(5L).build();
        EventAttendeeResponse attendee =
                new EventAttendeeResponse(5L, 1L, "Ana", LocalDateTime.now());

        given(eventRepository.existsById(eventId)).willReturn(true);
        given(eventRegistrationsRepository.findByEventIdWithUser(eventId, pageable))
                .willReturn(new PageImpl<>(List.of(registration), pageable, 1L));
        given(eventRegistrationMapper.toAttendee(registration)).willReturn(attendee);

        PageResponse<EventAttendeeResponse> result = eventService.findEventAttendees(eventId, pageable);

        assertThat(result.content()).containsExactly(attendee);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    void findEventAttendees_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long eventId = 99L;
        Pageable pageable = PageRequest.of(0, 20);

        given(eventRepository.existsById(eventId)).willReturn(false);

        assertThatThrownBy(() -> eventService.findEventAttendees(eventId, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event");

        then(eventRegistrationsRepository).shouldHaveNoInteractions();
    }

    @Test
    void registerUserForEvent_whenThereIsNoProfile_throwsResourceNotFoundBeforeLockingTheEvent() {
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.registerUserForEvent(USER_SUB, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        then(eventRepository).shouldHaveNoInteractions();
        then(registrationOutbox).shouldHaveNoInteractions();
    }

    @Test
    void registerUserForEvent_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long eventId = 99L;
        User user = validUser();
        user.setId(1L);
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.registerUserForEvent(USER_SUB, eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event");

        then(eventRegistrationsRepository).should(never()).save(any());
        then(registrationOutbox).shouldHaveNoInteractions();
    }

    @Test
    void findUserRegistration_whenThereIsNoProfile_throwsResourceNotFoundException() {
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findUserRegistration(10L, USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void unregisterUserFromEvent_whenThereIsNoProfile_throwsResourceNotFoundException() {
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.unregisterUserFromEvent(10L, USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        then(eventRegistrationsRepository).should(never()).delete(any());
    }

    @Test
    void registerUserForEvent_whenEventIsFull_throwsEventFullException() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).capacity(2).build();

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventIdIncludeDeleted(1L, eventId))
                .willReturn(Optional.empty());
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(2L);

        assertThatThrownBy(() -> eventService.registerUserForEvent(USER_SUB, eventId))
                .isInstanceOf(EventFullException.class)
                .hasMessageContaining("full");

        then(eventRegistrationsRepository).should(never()).save(any());
        then(registrationOutbox).shouldHaveNoInteractions();
    }

    @Test
    void registerUserForEvent_whenAlreadyRegisteredAndEventIsFull_throwsAlreadyRegisteredException() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).capacity(2).build();
        EventRegistration active = EventRegistration.builder().user(user).event(event).build();

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventIdIncludeDeleted(1L, eventId))
                .willReturn(Optional.of(active));

        assertThatThrownBy(() -> eventService.registerUserForEvent(USER_SUB, eventId))
                .isInstanceOf(AlreadyRegisteredException.class)
                .hasMessageContaining("already registered");

        then(eventRegistrationsRepository).should(never()).save(any());
    }

    @Test
    void registerUserForEvent_whenASpotWasFreed_registersTheUser() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(2L);
        Event event = Event.builder().id(eventId).capacity(2).build();
        EventRegistration saved = EventRegistration.builder().id(7L).user(user).event(event).build();
        EventRegistrationResponse expected = new EventRegistrationResponse(7L, eventId);

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventIdIncludeDeleted(2L, eventId))
                .willReturn(Optional.empty());
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(1L);
        given(eventRegistrationsRepository.save(any(EventRegistration.class))).willReturn(saved);
        given(eventRegistrationMapper.toResponse(saved)).willReturn(expected);

        EventRegistrationResponse result = eventService.registerUserForEvent(USER_SUB, eventId);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<EventRegistration> captor = ArgumentCaptor.forClass(EventRegistration.class);
        then(eventRegistrationsRepository).should().save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getEvent()).isSameAs(event);
        then(registrationOutbox).should().recordCreated(eventId, USER_SUB);
    }

    @Test
    void registerUserForEvent_whenCancelledAndEventHasRoom_restoresItAndRecordsTheMessage() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).capacity(2).build();
        EventRegistration cancelled = EventRegistration.builder()
                .user(user).event(event).deleted(true).build();

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventIdIncludeDeleted(1L, eventId))
                .willReturn(Optional.of(cancelled));
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(1L);
        given(eventRegistrationsRepository.save(cancelled)).willReturn(cancelled);
        EventRegistrationResponse expected = new EventRegistrationResponse(5L, eventId);
        given(eventRegistrationMapper.toResponse(cancelled)).willReturn(expected);

        EventRegistrationResponse result = eventService.registerUserForEvent(USER_SUB, eventId);

        assertThat(result).isSameAs(expected);
        assertThat(cancelled.isDeleted()).isFalse();
        then(registrationOutbox).should().recordCreated(eventId, USER_SUB);
    }

    @Test
    void registerUserForEvent_whenCancelledAndEventIsFull_throwsEventFullAndKeepsItCancelled() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).capacity(2).build();
        EventRegistration cancelled = EventRegistration.builder()
                .user(user).event(event).deleted(true).build();

        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(user));
        given(eventRepository.findByIdForUpdate(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventIdIncludeDeleted(1L, eventId))
                .willReturn(Optional.of(cancelled));
        given(eventRegistrationsRepository.countByEventId(eventId)).willReturn(2L);

        assertThatThrownBy(() -> eventService.registerUserForEvent(USER_SUB, eventId))
                .isInstanceOf(EventFullException.class);

        assertThat(cancelled.isDeleted()).isTrue();
        then(eventRegistrationsRepository).should(never()).save(any());
    }
}
