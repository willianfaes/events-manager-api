package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    EventMapper eventMapper;

    @InjectMocks
    EventService eventService;

    @Test
    void shouldReturnAllEvents() {

        Event fakeEvent = new Event();
        Pageable fakePageable = PageRequest.of(0, 10);
        Page<Event> fakePage = new PageImpl<>(
                List.of(fakeEvent),
                fakePageable,
                1L
        );
        EventResponse fakeResponse = EventResponse.builder().build();

        when(eventRepository.findAllWithAddress(fakePageable)).thenReturn(fakePage);
        when(eventMapper.toResponse(fakeEvent)).thenReturn(fakeResponse);

        PageResponse<EventResponse> result = eventService.findAllEvents(fakePageable);

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

      when(eventRepository.findAllWithAddress(fakePageable)).thenReturn(fakePage);

      PageResponse<EventResponse> result = eventService.findAllEvents(fakePageable);

      assertThat(result.content()).isEmpty();
      assertThat(result.page()).isZero();
      assertThat(result.size()).isEqualTo(10);
      assertThat(result.totalElements()).isZero();
      assertThat(result.totalPages()).isZero();
      assertThat(result.last()).isTrue();
    }
}
