package com.dayo.careround.roundservice.services;

import com.dayo.careround.roundservice.data.models.Round;
import com.dayo.careround.roundservice.data.models.RoundPatientReview;
import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import com.dayo.careround.roundservice.data.models.enums.RoundStatus;
import com.dayo.careround.roundservice.data.repositories.RoundPatientReviewRepository;
import com.dayo.careround.roundservice.data.repositories.RoundRepository;
import com.dayo.careround.roundservice.dto.request.CreateRoundRequest;
import com.dayo.careround.roundservice.dto.response.RoundResponse;
import com.dayo.careround.roundservice.exception.PatientNotFoundException;
import com.dayo.careround.roundservice.exception.RoundNotFoundException;
import com.dayo.careround.roundservice.grpc.PatientVerificationClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoundService {

    private final RoundRepository roundRepository;
    private final RoundPatientReviewRepository reviewRepository;
    private final PatientVerificationClient patientVerificationClient;

    @Transactional
    public RoundResponse create(CreateRoundRequest request) {
        for (UUID patientId : request.patientIds()) {
            if (!patientVerificationClient.patientExists(patientId)) {
                throw new PatientNotFoundException("No patient with id " + patientId + " exists");
            }
        }

        Round round = new Round();
        round.setWardId(request.wardId());
        round.setStatus(RoundStatus.IN_PROGRESS);
        roundRepository.save(round);

        for (UUID patientId : request.patientIds()) {
            RoundPatientReview review = new RoundPatientReview();
            review.setRoundId(round.getId());
            review.setPatientId(patientId);
            review.setState(RoundPatientReviewState.PENDING);
            reviewRepository.save(review);
        }

        return toResponse(round);
    }

    public RoundResponse getByPublicId(UUID publicId) {
        Round round = roundRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RoundNotFoundException("No round with that public id"));
        return toResponse(round);
    }

    /**
     * Called after any review reaches a terminal state (RESOLVED, CARRIED_FORWARD, DISCHARGED).
     * COMPLETED is a derived label — true once every review under the round is terminal.
     */
    @Transactional
    public void recheckCompletion(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new RoundNotFoundException("No round with that id"));

        if (round.getStatus() == RoundStatus.COMPLETED) return;

        List<RoundPatientReview> reviews = reviewRepository.findByRoundId(roundId);
        boolean allTerminal = reviews.stream().allMatch(review ->
                review.getState() == RoundPatientReviewState.RESOLVED
                        || review.getState() == RoundPatientReviewState.CARRIED_FORWARD
                        || review.getState() == RoundPatientReviewState.DISCHARGED);

        if (allTerminal && !reviews.isEmpty()) {
            round.setStatus(RoundStatus.COMPLETED);
            roundRepository.save(round);
        }
    }

    private RoundResponse toResponse(Round round) {
        return new RoundResponse(round.getPublicId(), round.getWardId(), round.getStatus());
    }
}