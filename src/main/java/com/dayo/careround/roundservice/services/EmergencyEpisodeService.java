package com.dayo.careround.roundservice.services;

import com.dayo.careround.roundservice.data.models.EmergencyEpisode;
import com.dayo.careround.roundservice.data.models.OutboxEvent;
import com.dayo.careround.roundservice.data.models.enums.OutboxStatus;
import com.dayo.careround.roundservice.data.repositories.EmergencyEpisodeRepository;
import com.dayo.careround.roundservice.data.repositories.OutboxEventRepository;
import com.dayo.careround.roundservice.dto.request.DeclareEmergencyRequest;
import com.dayo.careround.roundservice.dto.response.EmergencyEpisodeResponse;
import com.dayo.careround.roundservice.exception.DuplicateEmergencyException;
import com.dayo.careround.roundservice.exception.EmergencyAlreadyActiveException;
import com.dayo.careround.roundservice.exception.EmergencyEpisodeNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyEpisodeService {

    private final EmergencyEpisodeRepository emergencyEpisodeRepository;
    private final RoundPatientReviewService reviewService;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public EmergencyEpisodeResponse declare(UUID declaredBy, DeclareEmergencyRequest request) {
        emergencyEpisodeRepository.findByPatientIdAndResolvedAtIsNull(request.patientId())
                .ifPresent(existing -> {
                    throw new EmergencyAlreadyActiveException(
                            "This patient already has an active, unresolved emergency episode");
                });

        if (emergencyEpisodeRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateEmergencyException("This emergency declaration was already processed");
        }

        EmergencyEpisode episode = new EmergencyEpisode();
        episode.setPatientId(request.patientId());
        episode.setDeclaredBy(declaredBy);
        episode.setIdempotencyKey(request.idempotencyKey());

        // saveAndFlush so a concurrent duplicate's unique-key violation surfaces here, not at commit
        try {
            emergencyEpisodeRepository.saveAndFlush(episode);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmergencyException("This emergency declaration was already processed");
        }

        publishEmergencyDeclared(episode);

        reviewService.suspendForEmergency(request.patientId());

        return toResponse(episode);
    }

    @Transactional
    public EmergencyEpisodeResponse resolve(UUID episodeId) {
        EmergencyEpisode episode = emergencyEpisodeRepository.findById(episodeId)
                .orElseThrow(() -> new EmergencyEpisodeNotFoundException("No emergency episode with that id"));

        if (episode.getResolvedAt() != null) {
            return toResponse(episode); // already resolved — idempotent no-op, not an error
        }

        episode.setResolvedAt(Instant.now());
        emergencyEpisodeRepository.save(episode);

        reviewService.reopenAfterStabilization(episode.getPatientId());

        return toResponse(episode);
    }

    public EmergencyEpisodeResponse getActiveForPatient(UUID patientId) {
        EmergencyEpisode episode = emergencyEpisodeRepository
                .findByPatientIdAndResolvedAtIsNull(patientId)
                .orElseThrow(() -> new EmergencyEpisodeNotFoundException("No active emergency for this patient"));
        return toResponse(episode);
    }

    private void publishEmergencyDeclared(EmergencyEpisode episode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("patientId", episode.getPatientId().toString());
            payload.put("declaredBy", episode.getDeclaredBy().toString());
            payload.put("declaredAt", episode.getDeclaredAt().toString());

            OutboxEvent event = new OutboxEvent();
            event.setIdempotencyKey("emergency-declared-" + episode.getId());
            event.setEventType("PATIENT_EMERGENCY_DECLARED");
            event.setStatus(OutboxStatus.PENDING);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setAttempts(0);
            event.setNextRetryAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to build PATIENT_EMERGENCY_DECLARED outbox event: {}", e.getMessage(), e);
        }
    }

    private EmergencyEpisodeResponse toResponse(EmergencyEpisode episode) {
        return new EmergencyEpisodeResponse(
                episode.getId(), episode.getPatientId(), episode.getDeclaredBy(),
                episode.getDeclaredAt(), episode.getResolvedAt()
        );
    }
}