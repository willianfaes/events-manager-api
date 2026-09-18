package dev.will.eventsmanager.events;

import dev.will.eventsmanager.AbstractIntegrationTest;
import dev.will.eventsmanager.events.entity.Category;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventAddress;
import dev.will.eventsmanager.events.repository.EventRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EventRepository eventRepository;

    @Test
    @WithMockUser
    void getEventById_whenEventExists_returnsEventWithAddress() throws Exception {

        EventAddress address = EventAddress.builder()
                .street("123 Main St")
                .complement("Suite 100")
                .city("Sample City")
                .stateOrProvince("State")
                .postalCode("12345")
                .countryCode("US")
                .venueName("Sample Venue")
                .build();

        Event event = Event.builder()
                .name("Fake Event")
                .category(Category.CONCERT)
                .schedule(LocalDateTime.of(2026, 9, 18, 20, 0))
                .address(address)
                .build();

        Event saved = eventRepository.save(event);


        mockMvc.perform(get("/events/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.address.id").value(saved.getAddress().getId()));
    }

    @Test
    @WithMockUser
    void getEventById_whenEventDoesNotExist_returns404() throws Exception {
        mockMvc.perform(get("/events/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Event")));
    }
}
