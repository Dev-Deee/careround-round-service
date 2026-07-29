package com.dayo.careround.roundservice.services;

import com.dayo.careround.roundservice.data.models.EmergencyEpisode;
import com.dayo.careround.roundservice.data.repositories.EmergencyEpisodeRepository;
import com.dayo.careround.roundservice.dto.request.DeclareEmergencyRequest;
import com.dayo.careround.roundservice.dto.response.EmergencyEpisodeResponse;
import com.dayo.careround.roundservice.exception.DuplicateEmergencyException;
import com.dayo.careround.roundservice.exception.EmergencyAlreadyActiveException;
import com.dayo.careround.roundservice.exception.EmergencyEpisodeNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyEpisodeService {

    private final EmergencyEpisodeRepository emergencyEpisodeRepository;
    private final RoundPatientReviewService reviewService;

    @Transactional
    public EmergencyEpisodeResponse declare(UUID declaredBy, DeclareEmergencyRequest request) {
        emergencyEpisodeRepository.findByPatientIdAndResolvedAtIsNull(request.patientId())
                .ifPresent(existing -> {
                    throw new EmergencyAlreadyActiveException(
                            "This patient already has an active, unresolved emergency episode");
                });

        EmergencyEpisode episode = new EmergencyEpisode();
        episode.setPatientId(request.patientId());
        episode.setDeclaredBy(declaredBy);
        episode.setIdempotencyKey(request.idempotencyKey());

        try {
            emergencyEpisodeRepository.save(episode);
        } catch (DataIntegrityViolationException e) {
            // idempotencyKey unique constraint caught a retry/double-tap
            throw new DuplicateEmergencyException("This emergency declaration was already processed");
        }

        // TODO: emit PATIENT_EMERGENCY_DECLARED via outbox — notification-service consumes this
        // TODO: emit real-time ward alert (§6.7) — once realtime-service exists

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

    private EmergencyEpisodeResponse toResponse(EmergencyEpisode episode) {
        return new EmergencyEpisodeResponse(
                episode.getId(), episode.getPatientId(), episode.getDeclaredBy(),
                episode.getDeclaredAt(), episode.getResolvedAt()
        );
    }
}