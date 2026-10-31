package com.dayo.careround.roundservice.services;

import com.dayo.careround.roundservice.data.models.Round;
import com.dayo.careround.roundservice.data.models.RoundPatientReview;
import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import com.dayo.careround.roundservice.data.models.enums.RoundStatus;
import com.dayo.careround.roundservice.data.repositories.RoundPatientReviewRepository;
import com.dayo.careround.roundservice.data.repositories.RoundRepository;
import com.dayo.careround.roundservice.dto.response.RoundResponse;
import com.dayo.careround.roundservice.exception.NotAuthorizedToEndRoundException;
import com.dayo.careround.roundservice.exception.RoundNotFoundException;
import com.dayo.careround.roundservice.grpc.ChargeVerificationClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoundLifecycleService {

    private final RoundRepository roundRepository;
    private final RoundPatientReviewRepository reviewRepository;
    private final RoundPatientReviewService reviewService;
    private final ChargeVerificationClient chargeVerificationClient;
    private final RoundService roundService;

    /**
     * Explicitly ends a round. Any SUSPENDED review is carried forward into a
     * fresh round (same ward); the current round is marked COMPLETED regardless
     * of whether every review reached a terminal state naturally.
     */
    @Transactional
    public RoundResponse endRound(UUID roundPublicId, UUID callerUserId, String callerRole) {
        Round round = roundRepository.findByPublicId(roundPublicId)
                .orElseThrow(() -> new RoundNotFoundException("No round with that public id"));

        boolean authorized = "DOCTOR".equals(callerRole)
                || ("NURSE".equals(callerRole) && chargeVerificationClient.isActiveChargeNurse(round.getWardId(), callerUserId));

        if (!authorized) {
            throw new NotAuthorizedToEndRoundException("Only a doctor or the active charge nurse for this ward may end a round");
        }

        List<RoundPatientReview> suspended = reviewRepository.findByRoundId(round.getId()).stream()
                .filter(r -> r.getState() == RoundPatientReviewState.SUSPENDED)
                .toList();

        if (!suspended.isEmpty()) {
            Round newRound = new Round();
            newRound.setWardId(round.getWardId());
            newRound.setStatus(RoundStatus.IN_PROGRESS);
            newRound.setCreatedBy(callerUserId);
            newRound.setConductedBy(round.getConductedBy()); // carried-forward reviews stay with the same doctor
            newRound.setStartedAt(Instant.now());
            roundRepository.save(newRound);

            for (RoundPatientReview review : suspended) {
                reviewService.carryForward(review.getId(), newRound.getId());
            }
        }

        round.setStatus(RoundStatus.COMPLETED);
        round.setEndedAt(Instant.now());
        roundRepository.save(round);

        return roundService.toResponse(round);
    }
}