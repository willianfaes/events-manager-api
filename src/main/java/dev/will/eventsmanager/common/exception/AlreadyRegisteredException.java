package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class AlreadyRegisteredException extends ApplicationException {
    public AlreadyRegisteredException() {
        super("You are already registered for this event!", HttpStatus.CONFLICT);
    }
}
