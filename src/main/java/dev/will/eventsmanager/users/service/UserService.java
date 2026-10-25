package dev.will.eventsmanager.users.service;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.exception.ProfileAlreadyExistsException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.users.dto.request.CreateProfileRequest;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import dev.will.eventsmanager.users.dto.response.RegisteredEventsResponse;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.mapper.MyRegistrationMapper;
import dev.will.eventsmanager.users.mapper.ProfileMapper;
import dev.will.eventsmanager.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final EventRegistrationsRepository eventRegistrationsRepository;
    private final MyRegistrationMapper myRegistrationMapper;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    public ProfileResponse createProfile(String externalId, CreateProfileRequest request) {
        if (userRepository.existsByExternalId(externalId)) {
            throw new ProfileAlreadyExistsException();
        }
        User profile = profileMapper.toEntity(request);
        profile.setExternalId(externalId);
        return profileMapper.toResponse(userRepository.save(profile));
    }

    public ProfileResponse getProfile(String externalId) {
        User profile = userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", externalId));
        return profileMapper.toResponse(profile);
    }

    public PageResponse<MyRegistrationResponse> getUserRegisteredEvents(String externalId, Pageable pageable) {
        Page<EventRegistration> page = eventRegistrationsRepository.findByUserExternalIdWithEvent(externalId, pageable);
        return PageResponse.from(page.map(myRegistrationMapper::toMyRegistrationResponse));
    }

    public RegisteredEventsResponse findRegisteredEventIds(String externalId, Set<Long> eventIds) {
        return new RegisteredEventsResponse(
                eventRegistrationsRepository.findRegisteredEventIds(externalId, eventIds));
    }
}
