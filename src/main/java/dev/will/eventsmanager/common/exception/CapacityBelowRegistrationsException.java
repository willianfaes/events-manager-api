package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class CapacityBelowRegistrationsException extends ApplicationException {
    public CapacityBelowRegistrationsException(long activeRegistrations) {
        super("Capacity cannot be lower than the " + activeRegistrations + " active registrations",
                HttpStatus.CONFLICT);
    }
}
