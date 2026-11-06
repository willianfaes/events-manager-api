package dev.will.eventsmanager.users.service;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.exception.ProfileAlreadyExistsException;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.users.dto.request.ProfileRequest;
import dev.will.eventsmanager.users.dto.request.UserAddressRequest;
import dev.will.eventsmanager.users.dto.response.MyRegistrationResponse;
import dev.will.eventsmanager.users.dto.response.ProfileResponse;
import dev.will.eventsmanager.users.dto.response.RegisteredEventsResponse;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.users.entity.UserAddress;
import dev.will.eventsmanager.users.mapper.MyRegistrationMapper;
import dev.will.eventsmanager.users.mapper.ProfileMapper;
import dev.will.eventsmanager.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static dev.will.eventsmanager.TestFixtures.USER_SUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Coordinates MILANO = new Coordinates(45.4642, 9.1900);
    private static final Coordinates ROME = new Coordinates(41.9028, 12.4964);

    @Mock
    EventRegistrationsRepository eventRegistrationsRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ProfileMapper profileMapper;

    @Mock
    MyRegistrationMapper myRegistrationMapper;

    @InjectMocks
    UserService userService;

    @Test
    void findRegisteredEventIds_returnsTheIdsFoundByTheRepository() {
        Set<Long> requested = Set.of(1L, 2L, 3L, 5L);
        given(eventRegistrationsRepository.findRegisteredEventIds(USER_SUB, requested))
                .willReturn(List.of(2L, 5L));

        RegisteredEventsResponse result = userService.findRegisteredEventIds(USER_SUB, requested);

        assertThat(result.eventIds()).containsExactly(2L, 5L);
    }

    @Test
    void getUserRegisteredEvents_mapsTheRepositoryPageKeepingItsPaging() {
        Pageable pageable = PageRequest.of(1, 2);
        EventRegistration registration = EventRegistration.builder().id(7L).build();
        MyRegistrationResponse mapped = MyRegistrationResponse.builder().registrationId(7L).build();
        given(eventRegistrationsRepository.findByUserExternalIdWithEvent(USER_SUB, pageable))
                .willReturn(new PageImpl<>(List.of(registration), pageable, 3));
        given(myRegistrationMapper.toMyRegistrationResponse(registration)).willReturn(mapped);

        PageResponse<MyRegistrationResponse> result = userService.getUserRegisteredEvents(USER_SUB, pageable);

        assertThat(result.content()).containsExactly(mapped);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    void createProfile_linksTheNewProfileToTheSub() {
        ProfileRequest request = profileRequest();
        User profile = User.builder().name("Demo User").address(UserAddress.builder().build()).build();
        ProfileResponse expected = new ProfileResponse("Demo User", 30, null);
        given(userRepository.existsByExternalId(USER_SUB)).willReturn(false);
        given(profileMapper.toEntity(request)).willReturn(profile);
        given(userRepository.save(profile)).willReturn(profile);
        given(profileMapper.toResponse(profile)).willReturn(expected);

        ProfileResponse result = userService.createProfile(USER_SUB, request, new Coordinates(45.4716, 9.1889));

        assertThat(result).isEqualTo(expected);
        assertThat(profile.getExternalId()).isEqualTo(USER_SUB);
        assertThat(profile.getAddress().getLatitude()).isEqualTo(45.4716);
    }

    @Test
    void createProfile_rejectsASecondProfileForTheSameSub() {
        given(userRepository.existsByExternalId(USER_SUB)).willReturn(true);

        assertThatThrownBy(() -> userService.createProfile(USER_SUB, profileRequest(), null))
                .isInstanceOf(ProfileAlreadyExistsException.class);
        then(userRepository).should(never()).save(any());
    }

    @Test
    void getProfile_returnsTheProfileLinkedToTheSub() {
        User profile = User.builder().name("Demo User").build();
        ProfileResponse expected = new ProfileResponse("Demo User", 30, null);
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(profile));
        given(profileMapper.toResponse(profile)).willReturn(expected);

        assertThat(userService.getProfile(USER_SUB)).isEqualTo(expected);
    }

    @Test
    void getProfile_throwsNotFoundWhenTheSubHasNoProfile() {
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(USER_SUB))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_appliesTheRequestAndSavesTheNewCoordinates() {
        ProfileRequest request = profileRequest();
        User profile = profileInMilano();
        ProfileResponse expected = new ProfileResponse("Demo User", 30, null);
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(profile));
        given(profileMapper.toResponse(profile)).willReturn(expected);

        ProfileResponse result = userService.updateProfile(USER_SUB, request, ROME);

        assertThat(result).isEqualTo(expected);
        then(profileMapper).should().updateEntity(request, profile);
        assertThat(profile.getAddress().coordinates()).isEqualTo(ROME);
    }

    @Test
    void updateProfile_whenTheLookupFailsOnTheSameArea_keepsTheCoordinates() {
        User profile = profileInMilano();
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(profile));

        userService.updateProfile(USER_SUB, profileRequest(), null);

        assertThat(profile.getAddress().coordinates()).isEqualTo(MILANO);
    }

    @Test
    void updateProfile_whenTheLookupFailsOnANewArea_clearsTheCoordinates() {
        ProfileRequest request = profileRequest();
        User profile = profileInMilano();
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.of(profile));
        willAnswer(invocation -> {
            profile.getAddress().setCity("Rome");
            return null;
        }).given(profileMapper).updateEntity(request, profile);

        userService.updateProfile(USER_SUB, request, null);

        assertThat(profile.getAddress().coordinates()).isNull();
    }

    @Test
    void updateProfile_throwsNotFoundWhenTheSubHasNoProfile() {
        given(userRepository.findByExternalId(USER_SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(USER_SUB, profileRequest(), ROME))
                .isInstanceOf(ResourceNotFoundException.class);
        then(profileMapper).should(never()).updateEntity(any(), any());
    }

    private static User profileInMilano() {
        UserAddress address = UserAddress.builder().postalCode("20121").city("Milano").countryCode("IT").build();
        address.locateAt(MILANO);
        return User.builder().name("Demo User").address(address).build();
    }

    private static ProfileRequest profileRequest() {
        return new ProfileRequest("Demo User", 30,
                new UserAddressRequest("Via del Corso 1", null, "Rome", null, "00186", "IT"));
    }
}
