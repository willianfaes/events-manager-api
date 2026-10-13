package dev.will.eventsmanager.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.BDDAssertions.then;

class GlobalExceptionHandlerTest {

    private static final String PATH = "/events/1";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", PATH);

    @Test
    @DisplayName("Exception - should return 500 with a generic message and not leak the original one")
    void handleGenericException_should_returnInternalServerErrorWithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("secret detail"), request);

        ErrorResponse body = requireNonNull(response.getBody());
        then(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        then(body.status()).isEqualTo(500);
        then(body.message()).isEqualTo("An unexpected error occurred");
        then(body.path()).isEqualTo(PATH);
    }

    @Test
    @DisplayName("DataIntegrityViolationException - should return 409 without exposing the database message")
    void handleDataIntegrityViolationException_should_returnConflictWithGenericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("duplicate key value violates constraint"), request);

        ErrorResponse body = requireNonNull(response.getBody());
        then(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        then(body.message()).isEqualTo("Data integrity violation");
        then(body.path()).isEqualTo(PATH);
    }
}
