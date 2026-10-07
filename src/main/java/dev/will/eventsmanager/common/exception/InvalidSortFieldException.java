package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidSortFieldException extends ApplicationException {
    public InvalidSortFieldException(String notAllowed) {
        super(notAllowed + " is not an allowed sort field.", HttpStatus.BAD_REQUEST);
    }
}
