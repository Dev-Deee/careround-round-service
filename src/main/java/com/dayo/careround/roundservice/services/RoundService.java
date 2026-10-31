package com.dayo.careround.roundservice.services;

import com.dayo.careround.roundservice.data.models.Round;
import com.dayo.careround.roundservice.data.models.RoundPatientReview;
import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import com.dayo.careround.roundservice.data.models.enums.RoundStatus;
import com.dayo.careround.roundservice.data.repositories.RoundPatientReviewRepository;
import com.dayo.careround.roundservice.data.repositories.RoundRepository;
import com.dayo.careround.roundservice.dto.request.CreateRoundRequest;
import com.dayo.careround.roundservice.dto.response.PageResponse;
import com.dayo.careround.roundservice.dto.response.ReviewCounts;
import com.dayo.careround.roundservice.dto.response.RoundResponse;
import com.dayo.careround.roundservice.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.dayo.careround.roundservice.exception.PatientNotFoundException;
import com.dayo.careround.roundservice.exception.WardNotFoundException;
import com.dayo.careround.roundservice.exception.RoundNotFoundException;
import com.dayo.careround.roundservice.grpc.PatientVerificationClient;
import com.dayo.careround.roundservice.grpc.WardVerificationClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoundService {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private final RoundRepository roundRepository;
    private final RoundPatientReviewRepository reviewRepository;
    private final PatientVerificationClient patientVerificationClient;
    private final WardVerificationClient wardVerificationClient;

    @Transactional
    public RoundResponse create(CreateRoundRequest request, UUID callerId, String callerRole) {
        if (!wardVerificationClient.wardExists(request.wardId())) {
            throw new WardNotFoundException("No ward with that id");
        }
        for (UUID patientId : request.patientIds()) {
            if (!patientVerificationClient.patientExists(patientId)) {
                throw new PatientNotFoundException("No patient with id " + patientId + " exists");
            }
        }

        Round round = new Round();
        round.setWardId(request.wardId());
        round.setStatus(RoundStatus.IN_PROGRESS);
        round.setCreatedBy(callerId);
        round.setConductedBy("DOCTOR".equals(callerRole) ? callerId : null);
        round.setStartedAt(Instant.now()); // rounds start IN_PROGRESS; there is no separate start step
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
            round.setEndedAt(Instant.now());
            roundRepository.save(round);
        }
    }

    // Filtered, paged round list, newest first. Authorisation (who may ask for which doctor/ward) is the caller's job.
    @Transactional
    public PageResponse<RoundResponse> list(UUID wardId, UUID doctorId, RoundStatus status, Instant from, Instant to,
                                            Integer page, Integer size) {
        if (from != null && to != null && !from.isBefore(to)) throw new BadRequestException("from must be before to");
        Specification<Round> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (wardId != null) p.add(cb.equal(root.get("wardId"), wardId));
            if (doctorId != null) p.add(cb.equal(root.get("conductedBy"), doctorId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) p.add(cb.lessThan(root.get("createdAt"), to));
            return cb.and(p.toArray(Predicate[]::new));
        };
        Pageable pageable = PageRequest.of(page == null ? 0 : Math.max(page, 0),
                size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE),
                Sort.by(Sort.Order.desc("createdAt").nullsLast(), Sort.Order.asc("id")));
        Page<Round> rounds = roundRepository.findAll(spec, pageable);
        Map<UUID, ReviewCounts> counts = reviewCounts(rounds.map(Round::getId).getContent());
        return PageResponse.of(rounds, r -> toResponse(r, counts.get(r.getId())));
    }

    public RoundResponse toResponse(Round round) {
        return toResponse(round, reviewCounts(List.of(round.getId())).get(round.getId()));
    }

    private RoundResponse toResponse(Round round, ReviewCounts counts) {
        return new RoundResponse(round.getPublicId(), round.getWardId(), round.getStatus(), round.getCreatedBy(),
                round.getConductedBy(), round.getCreatedAt(), round.getStartedAt(), round.getEndedAt(),
                counts != null ? counts : new ReviewCounts(0, 0, 0));
    }

    private Map<UUID, ReviewCounts> reviewCounts(List<UUID> roundIds) {
        if (roundIds.isEmpty()) return Map.of();
        Map<UUID, long[]> raw = new HashMap<>();
        for (Object[] row : reviewRepository.countByRoundAndState(roundIds)) {
            long[] c = raw.computeIfAbsent((UUID) row[0], k -> new long[3]);
            switch ((RoundPatientReviewState) row[1]) {
                case PENDING -> c[0] += (Long) row[2];
                case SUSPENDED -> c[1] += (Long) row[2];
                case RESOLVED -> c[2] += (Long) row[2];
                default -> { }
            }
        }
        Map<UUID, ReviewCounts> out = new HashMap<>();
        raw.forEach((id, c) -> out.put(id, new ReviewCounts(c[0], c[1], c[2])));
        return out;
    }
}