package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class ProfileAlreadyExistsException extends ApplicationException {
    public ProfileAlreadyExistsException() {
        super("You already have a profile!", HttpStatus.CONFLICT);
    }
}
