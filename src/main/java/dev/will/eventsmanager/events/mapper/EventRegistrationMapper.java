package dev.will.eventsmanager.events.mapper;

import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.entity.EventRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface EventRegistrationMapper {

    EventRegistrationResponse toResponse(EventRegistration registration);
}