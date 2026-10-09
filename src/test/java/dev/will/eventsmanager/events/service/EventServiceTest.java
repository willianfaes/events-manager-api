package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.events.dto.request.EventAddressRequest;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.request.EventFilter;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static dev.will.eventsmanager.TestFixtures.USER_EMAIL;
import static dev.will.eventsmanager.TestFixtures.validUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    EventMapper eventMapper;

    @Mock
    EventRegistrationsRepository eventRegistrationsRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    EventService eventService;

    @Test
    void shouldReturnAllEvents() {

        Event fakeEvent = new Event();
        Pageable fakePageable = PageRequest.of(0, 10);
        EventFilter filter = new EventFilter("test", null, null);
        Page<Event> fakePage = new PageImpl<>(
                List.of(fakeEvent),
                fakePageable,
                1L
        );
        EventResponse fakeResponse = EventResponse.builder().build();

        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), eq(fakePageable))).thenReturn(fakePage);
        when(eventMapper.toResponse(fakeEvent)).thenReturn(fakeResponse);

        PageResponse<EventResponse> result = eventService.findAllEvents(fakePageable, filter);

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
    void shouldReturnEmptyListWhenNoEventsExist() {

        Pageable fakePageable = PageRequest.of(0, 10);
        Page<Event> fakePage = Page.empty(fakePageable);

        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), eq(fakePageable)))
                .thenReturn(fakePage);

        EventFilter filter = new EventFilter("test", null, null);
        PageResponse<EventResponse> result = eventService.findAllEvents(fakePageable, filter);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.last()).isTrue();
    }

    @Test
    void findEventById_whenEventExists_returnsMappedResponse() {
        Long id = 1L;
        Event event = Event.builder().id(id).build();
        EventResponse expectedResponse = EventResponse.builder().id(id).build();

        when(eventRepository.findByIdWithAddress(id)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        EventResponse result = eventService.findEventById(id);

        assertThat(result).isEqualTo(expectedResponse);
        verify(eventRepository).findByIdWithAddress(id);
        verify(eventMapper).toResponse(event);
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

        Event toPersist = Event.builder().build();
        Event saved = Event.builder().id(1L).build();
        EventResponse expected = EventResponse.builder().id(1L).build();

        when(eventMapper.toEntity(request)).thenReturn(toPersist);
        when(eventRepository.save(toPersist)).thenReturn(saved);
        when(eventMapper.toResponse(saved)).thenReturn(expected);

        EventResponse result = eventService.createEvent(request);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void unregisterUserFromEvent_whenActiveRegistration_softDeletesIt() {
        Long eventId = 10L;
        User user = validUser();
        user.setId(1L);
        Event event = Event.builder().id(eventId).build();
        EventRegistration registration = EventRegistration.builder().user(user).event(event).build();

        given(userRepository.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventId(1L, eventId)).willReturn(Optional.of(registration));

        eventService.unregisterUserFromEvent(eventId, USER_EMAIL);

        then(eventRegistrationsRepository).should().delete(registration);
    }

    @Test
    void unregisterUserFromEvent_whenEventDoesNotExist_throwsResourceNotFoundException() {
        Long eventId = 99L;
        User user = validUser();
        user.setId(1L);

        given(userRepository.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.unregisterUserFromEvent(eventId, USER_EMAIL))
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

        given(userRepository.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        given(eventRegistrationsRepository.findByUserIdAndEventId(1L, eventId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.unregisterUserFromEvent(eventId, USER_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Registration");

        then(eventRegistrationsRepository).should(never()).delete(any());
    }
}
