package com.dayo.careround.roundservice.services;

import com.dayo.careround.roundservice.data.models.EmergencyEpisode;
import com.dayo.careround.roundservice.data.models.OutboxEvent;
import com.dayo.careround.roundservice.data.models.Round;
import com.dayo.careround.roundservice.data.models.RoundPatientReview;
import com.dayo.careround.roundservice.data.models.enums.OutboxStatus;
import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import com.dayo.careround.roundservice.data.models.enums.RoundStatus;
import com.dayo.careround.roundservice.data.repositories.EmergencyEpisodeRepository;
import com.dayo.careround.roundservice.data.repositories.OutboxEventRepository;
import com.dayo.careround.roundservice.data.repositories.RoundPatientReviewRepository;
import com.dayo.careround.roundservice.data.repositories.RoundRepository;
import com.dayo.careround.roundservice.dto.request.ResolveReviewRequest;
import com.dayo.careround.roundservice.dto.response.RoundPatientReviewResponse;
import com.dayo.careround.roundservice.exception.ReviewNotFoundException;
import com.dayo.careround.roundservice.exception.ReviewNotResolvableException;
import com.dayo.careround.roundservice.exception.RoundNotFoundException;
import com.dayo.careround.roundservice.grpc.CareTaskQueryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import com.dayo.careround.roundservice.data.models.Note;
import com.dayo.careround.roundservice.data.models.enums.NoteContext;
import com.dayo.careround.roundservice.exception.NoteReviewMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundPatientReviewService {

    private final RoundPatientReviewRepository reviewRepository;
    private final EmergencyEpisodeRepository emergencyEpisodeRepository;
    private final RoundService roundService;
    private final RoundRepository roundRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CareTaskQueryClient careTaskQueryClient;
    public List<RoundPatientReviewResponse> getByRoundPublicId(UUID roundPublicId) {
        Round round = roundRepository.findByPublicId(roundPublicId)
                .orElseThrow(() -> new RoundNotFoundException("No round with that public id"));
        return getByRound(round.getId());
    }


    public List<RoundPatientReviewResponse> getByRound(UUID roundId) {
        return reviewRepository.findByRoundId(roundId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Acknowledges every care task currently pending for the patient. Tasks created afterwards are
     * not covered and block resolve until tasks are acknowledged again.
     */
    @Transactional
    public void acknowledgeTasks(UUID reviewId) {
        RoundPatientReview review = findOrThrow(reviewId);
        review.setAcknowledgedTaskIds(new HashSet<>(careTaskQueryClient.getPendingTaskIds(review.getPatientId())));
        review.setTasksAcknowledged(true);
        reviewRepository.save(review);
    }

    // Called when a note is committed with ?reviewId: the note must be this review's patient's ROUND note, and the
    // review must still be open. A note for patient A can no longer satisfy patient B's review.
    public void attachNote(UUID reviewId, Note note) {
        RoundPatientReview review = findOrThrow(reviewId);
        if (!review.getPatientId().equals(note.getPatientId())) {
            throw new NoteReviewMismatchException("This note is for a different patient than the review");
        }
        if (note.getContext() != NoteContext.ROUND) {
            throw new NoteReviewMismatchException("Only a ROUND note can satisfy a round review (this note is " + note.getContext() + ")");
        }
        if (review.getState() != RoundPatientReviewState.PENDING && review.getState() != RoundPatientReviewState.SUSPENDED) {
            throw new NoteReviewMismatchException("This review is already " + review.getState());
        }
        review.setClinicalNoteId(note.getId());
        reviewRepository.save(review);
    }

    /**
     * Three-gate completion: every pending care task acknowledged (FR-1.3a) + clinical note committed
     * + status summary set.
     * Note commitment itself is verified by the caller (NoteService) before this is invoked —
     * this method assumes clinicalNoteId already points to a COMMITTED note.
     */
    @Transactional
    public RoundPatientReviewResponse resolve(UUID reviewId, UUID doctorId, ResolveReviewRequest request) {
        RoundPatientReview review = findOrThrow(reviewId);

        if (review.getState() != RoundPatientReviewState.PENDING) {
            throw new ReviewNotResolvableException(
                    "Only a PENDING review can be resolved — current state: " + review.getState());
        }
        if (!review.isTasksAcknowledged()) {
            throw new ReviewNotResolvableException("Tasks must be acknowledged before resolving");
        }
        Set<UUID> unacknowledged = new HashSet<>(careTaskQueryClient.getPendingTaskIds(review.getPatientId()));
        unacknowledged.removeAll(review.getAcknowledgedTaskIds());
        if (!unacknowledged.isEmpty()) {
            throw new ReviewNotResolvableException(unacknowledged.size()
                    + " pending care task(s) not acknowledged — acknowledge tasks again before resolving: " + unacknowledged);
        }
        if (review.getClinicalNoteId() == null) {
            throw new ReviewNotResolvableException("A committed clinical note is required before resolving");
        }

        review.setState(RoundPatientReviewState.RESOLVED);
        review.setStatusSummary(request.statusSummary());
        review.setResolvedByDoctorId(doctorId);
        review.setReviewedAt(Instant.now());
        reviewRepository.save(review);
        roundRepository.findById(review.getRoundId())
                .filter(round -> round.getConductedBy() == null)
                .ifPresent(round -> { round.setConductedBy(doctorId); roundRepository.save(round); });

        publishReviewCompleted(review);

        roundService.recheckCompletion(review.getRoundId());
        return toResponse(review);
    }

    /**
     * Called when a patient's EmergencyEpisode is declared.
     */
    @Transactional
    public void suspendForEmergency(UUID patientId) {
        reviewRepository.findByPatientIdAndState(patientId, RoundPatientReviewState.PENDING).stream()
                .filter(this::isInActiveRound)
                .forEach(review -> {
                    review.setState(RoundPatientReviewState.SUSPENDED);
                    reviewRepository.save(review);
                });
    }

    /**
     * Called when an EmergencyEpisode resolves — reopens the review in the SAME round.
     */
    @Transactional
    public void reopenAfterStabilization(UUID patientId) {
        reviewRepository.findByPatientIdAndState(patientId, RoundPatientReviewState.SUSPENDED).stream()
                .filter(this::isInActiveRound)
                .forEach(review -> {
                    review.setState(RoundPatientReviewState.PENDING);
                    reviewRepository.save(review);
                });
    }

    /**
     * Called when a patient is discharged before their review resolves.
     */
    @Transactional
    public void closeForDischarge(UUID patientId) {
        reviewRepository.findByPatientIdAndState(patientId, RoundPatientReviewState.PENDING).stream()
                .filter(this::isInActiveRound)
                .forEach(review -> {
                    review.setState(RoundPatientReviewState.DISCHARGED);
                    reviewRepository.save(review);
                    roundService.recheckCompletion(review.getRoundId());
                });
    }

    /**
     * Called when a round ends while a review is still SUSPENDED — carries it forward
     * to a fresh review in the next round, per the locked state machine.
     */
    @Transactional
    public void carryForward(UUID reviewId, UUID newRoundId) {
        RoundPatientReview review = findOrThrow(reviewId);

        if (review.getState() != RoundPatientReviewState.SUSPENDED) {
            throw new ReviewNotResolvableException(
                    "Only a SUSPENDED review can be carried forward — current state: " + review.getState());
        }

        review.setState(RoundPatientReviewState.CARRIED_FORWARD);
        reviewRepository.save(review);

        RoundPatientReview freshReview = new RoundPatientReview();
        freshReview.setRoundId(newRoundId);
        freshReview.setPatientId(review.getPatientId());
        freshReview.setState(RoundPatientReviewState.PENDING);
        reviewRepository.save(freshReview);

        roundService.recheckCompletion(review.getRoundId());
    }

    private RoundPatientReview findOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("No review with that id"));
    }

    private void publishReviewCompleted(RoundPatientReview review) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("patientId", review.getPatientId().toString());
            payload.put("roundId", review.getRoundId().toString());
            payload.put("statusSummary", review.getStatusSummary());
            payload.put("resolvedByDoctorId", review.getResolvedByDoctorId().toString());
            Round round = roundRepository.findById(review.getRoundId()).orElse(null);
            payload.put("wardId", round != null ? round.getWardId().toString() : null);
            payload.put("roundPublicId", round != null ? round.getPublicId().toString() : null);
            payload.put("occurredAt", (review.getReviewedAt() != null ? review.getReviewedAt() : Instant.now()).toString());

            OutboxEvent event = new OutboxEvent();
            event.setIdempotencyKey("review-completed-" + review.getId());
            event.setEventType("PATIENT_REVIEW_COMPLETED");
            event.setStatus(OutboxStatus.PENDING);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setAttempts(0);
            event.setNextRetryAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to build PATIENT_REVIEW_COMPLETED outbox event: {}", e.getMessage(), e);
        }
    }

    private RoundPatientReviewResponse toResponse(RoundPatientReview review) {
        return new RoundPatientReviewResponse(
                review.getId(), review.getRoundId(), review.getPatientId(),
                review.getState(), review.isTasksAcknowledged(), review.getClinicalNoteId(),
                review.getStatusSummary(), review.getResolvedByDoctorId(), review.getReviewedAt(),
                roundRepository.findById(review.getRoundId()).map(Round::getPublicId).orElse(null)
        );
    }

    private boolean isInActiveRound(RoundPatientReview review) {
        return roundRepository.findById(review.getRoundId())
                .map(round -> round.getStatus() == RoundStatus.IN_PROGRESS)
                .orElse(false);
    }
}