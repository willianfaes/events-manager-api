package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class EventHasActiveRegistrationsException extends ApplicationException {
    public EventHasActiveRegistrationsException(long activeRegistrations) {
        super("Event has " + activeRegistrations + " active registrations and cannot be deleted",
                HttpStatus.CONFLICT);
    }
}
