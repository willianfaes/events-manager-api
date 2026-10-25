package dev.will.eventsmanager.events.mapper;

import dev.will.eventsmanager.events.dto.response.EventAttendeeResponse;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.entity.EventRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface EventRegistrationMapper {

    @Mapping(source = "event.id", target = "eventId")
    EventRegistrationResponse toResponse(EventRegistration registration);

    @Mapping(source = "id", target = "registrationId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "name")
    EventAttendeeResponse toAttendee(EventRegistration registration);
}