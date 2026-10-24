package dev.will.eventsmanager.users.service;

import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.users.dto.response.RegisteredEventsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static dev.will.eventsmanager.TestFixtures.USER_SUB;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    EventRegistrationsRepository eventRegistrationsRepository;

    @InjectMocks
    UserService userService;

    @Test
    void findRegisteredEventIds_returnsTheIdsFoundByTheRepository() {
        Set<Long> requested = Set.of(1L, 2L, 3L, 5L);
        given(eventRegistrationsRepository.findRegisteredEventIds(USER_SUB, requested))
                .willReturn(List.of(2L, 5L));

        RegisteredEventsResponse result = userService.findRegisteredEventIds(USER_SUB, requested);

        then(result.eventIds()).containsExactly(2L, 5L);
    }
}
