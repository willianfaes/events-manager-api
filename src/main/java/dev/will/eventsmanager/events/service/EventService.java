package dev.will.eventsmanager.events.service;

import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.exception.ResourceNotFoundException;
import dev.will.eventsmanager.events.dto.request.EventCreateRequest;
import dev.will.eventsmanager.events.dto.response.EventRegistrationResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import dev.will.eventsmanager.events.entity.Event;
import dev.will.eventsmanager.events.entity.EventRegistration;
import dev.will.eventsmanager.users.entity.User;
import dev.will.eventsmanager.core.exceptions.BusinessException;
import dev.will.eventsmanager.events.mapper.EventMapper;
import dev.will.eventsmanager.events.mapper.EventRegistrationMapper;
import dev.will.eventsmanager.events.repository.EventRegistrationsRepository;
import dev.will.eventsmanager.events.repository.EventRepository;
import dev.will.eventsmanager.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional
public class EventService {

    private final EventMapper eventMapper;
    private final EventRegistrationMapper eventRegistrationMapper;
    private final EventRepository eventRepository;
    private final EventRegistrationsRepository eventRegistrationsRepository;
    private final UserRepository userRepository;

    public PageResponse<EventResponse> findAllEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findAllWithAddress(pageable);
        return PageResponse.from(events.map(eventMapper::toResponse));
    }

    public EventResponse findEventById(Long id) throws ResourceNotFoundException {
        Event event = eventRepository.findByIdWithAddress(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        return eventMapper.toResponse(event);
    }

    public EventResponse createEvent(EventCreateRequest eventCreateRequest) {
        Event entity = eventMapper.toEntity(eventCreateRequest);
        Event saved = eventRepository.save(entity);
        return eventMapper.toResponse(saved);
    }

    public EventRegistrationResponse registerUserForEvent(String userEmail, Long eventId) {

        // 1. Busca o Usuário completo usando o e-mail extraído do Token
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado!"));

        // 2. Busca o Evento completo (afinal, não dá pra se inscrever no que não existe)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("Evento não encontrado!"));

        // 3. A ÚNICA ida ao banco para checar inscrições usando os IDs
        Optional<EventRegistration> existingOpt = eventRegistrationsRepository
                .findByUserIdAndEventIdIncludeDeleted(user.getId(), event.getId());

        // 4. Se achou alguma coisa na caixa...
        if (existingOpt.isPresent()) {
            EventRegistration registration = existingOpt.get();

            // O cara tá vivinho da silva?
            if (!registration.isDeleted()) {
                throw new BusinessException("Você já está inscrito neste evento!");
            }

            // Se chegou aqui, é fantasma. REVIVE O CARA!
            registration.setDeleted(false);
            return eventRegistrationMapper.toResponse(eventRegistrationsRepository.save(registration));
        }

        // 5. Se a caixa estava vazia, é inscrição 100% nova.
        EventRegistration newRegistration = new EventRegistration();
        newRegistration.setUser(user);   // Setamos a entidade User inteira
        newRegistration.setEvent(event); // Setamos a entidade Event inteira

        // (Se a sua entidade tiver uma data de inscrição, você setaria aqui:
        // newRegistration.setRegistrationDate(LocalDateTime.now());)

        // Salva no banco, passa no Mapper e devolve pro Controller
        return eventRegistrationMapper.toResponse(eventRegistrationsRepository.save(newRegistration));
    }

    public void unregisterUserFromEvent(Long eventId, String userEmail) {
        // Stub: does nothing
    }
}
