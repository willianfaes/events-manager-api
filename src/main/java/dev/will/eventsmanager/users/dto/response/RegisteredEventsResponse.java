package dev.will.eventsmanager.users.dto.response;

import java.util.List;

public record RegisteredEventsResponse(
        List<Long> eventIds
) {
}
