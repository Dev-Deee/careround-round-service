package com.dayo.careround.roundservice.data.repositories;

import com.dayo.careround.roundservice.data.models.RoundPatientReview;
import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundPatientReviewRepository extends JpaRepository<RoundPatientReview, UUID> {
    List<RoundPatientReview> findByRoundId(UUID roundId);
    Optional<RoundPatientReview> findByRoundIdAndPatientId(UUID roundId, UUID patientId);
    List<RoundPatientReview> findByPatientIdAndState(UUID patientId, RoundPatientReviewState state);
}