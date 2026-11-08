package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class DemoProfileReadOnlyException extends ApplicationException {
    public DemoProfileReadOnlyException() {
        super("The demo profile is read-only", HttpStatus.FORBIDDEN);
    }
}
