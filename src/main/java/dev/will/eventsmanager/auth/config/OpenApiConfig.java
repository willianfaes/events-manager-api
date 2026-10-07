package dev.will.eventsmanager.auth.config;

import dev.will.eventsmanager.common.pagination.AllowedSortFields;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OperationCustomizer sortFieldsDoc() {
        return (operation, handlerMethod) -> {
            AllowedSortFields allowed = handlerMethod.getMethodAnnotation(AllowedSortFields.class);
            if (allowed != null && operation.getParameters() != null) {
                operation.getParameters().stream()
                        .filter(p -> "sort".equals(p.getName()))
                        .forEach(p -> p.setDescription(
                                "Format: field,direction (e.g., createdAt,desc). Allowed fields: "
                                        + String.join(", ", allowed.value())));
            }
            return operation;
        };
    }
}
