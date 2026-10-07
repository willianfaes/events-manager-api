package dev.will.eventsmanager.common.docs;

import dev.will.eventsmanager.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "403", description = "Caller lacks the required role",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public @interface AdminOnly {
}
