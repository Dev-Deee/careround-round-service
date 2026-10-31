package com.dayo.careround.roundservice.data.repositories;

import com.dayo.careround.roundservice.data.models.RoundPatientReview;
import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundPatientReviewRepository extends JpaRepository<RoundPatientReview, UUID> {
    List<RoundPatientReview> findByRoundId(UUID roundId);

    @Query("select r.roundId, r.state, count(r) from RoundPatientReview r where r.roundId in :roundIds group by r.roundId, r.state")
    List<Object[]> countByRoundAndState(@Param("roundIds") Collection<UUID> roundIds);
    Optional<RoundPatientReview> findByRoundIdAndPatientId(UUID roundId, UUID patientId);
    List<RoundPatientReview> findByPatientIdAndState(UUID patientId, RoundPatientReviewState state);
}