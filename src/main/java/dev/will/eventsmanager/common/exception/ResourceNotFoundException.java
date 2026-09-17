package dev.will.eventsmanager.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {

  public ResourceNotFoundException(String resourceName, Object id) {
    super(resourceName + " not found with id: " + id, HttpStatus.NOT_FOUND);
  }
}