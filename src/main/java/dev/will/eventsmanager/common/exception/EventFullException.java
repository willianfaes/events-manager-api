package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class EventFullException extends ApplicationException {
    public EventFullException() {
        super("This event is full!", HttpStatus.CONFLICT);
    }
}
