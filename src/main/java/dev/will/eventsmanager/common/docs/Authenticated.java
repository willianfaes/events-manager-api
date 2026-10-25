package dev.will.eventsmanager.common.docs;

import dev.will.eventsmanager.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
// Either scheme authorizes the call: a pasted token or the Cognito login.
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cognito")
@ApiResponse(responseCode = "401", description = "Missing or invalid token",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public @interface Authenticated {
}
