package dev.will.eventsmanager.users.mapper;

import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EventSummaryMapper.class})
public interface MyRegistrationMapper {

    @Mapping(target = "registrationId", source = "id")
    MyRegistrationResponse toMyRegistrationResponse(EventRegistration eventRegistration);
}
