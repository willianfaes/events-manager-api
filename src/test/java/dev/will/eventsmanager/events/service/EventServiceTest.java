package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        List<Event> fakeEvents = List.of(fakeEvent);
        EventResponse fakeResponse = EventResponse.builder().build();

        when(eventRepository.findAllWithAddress()).thenReturn(fakeEvents);
        when(eventMapper.toResponse(fakeEvent)).thenReturn(fakeResponse);

        List<EventResponse> result = eventService.findAllEvents();

        assertThat(result)
                .hasSize(1)
                .containsExactly(fakeResponse);
    }
}
