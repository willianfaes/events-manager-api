package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class ProfileWithoutCoordinatesException extends ApplicationException {
    public ProfileWithoutCoordinatesException() {
        super("The address of this profile could not be located", HttpStatus.CONFLICT);
    }
}
