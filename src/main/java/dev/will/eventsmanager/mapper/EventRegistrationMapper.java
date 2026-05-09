package dev.will.eventsmanager.mapper;

import dev.will.eventsmanager.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.entity.EventRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface EventRegistrationMapper {

    EventRegistrationResponse toResponse(EventRegistration registration);
}