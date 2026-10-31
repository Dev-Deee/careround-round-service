package com.dayo.careround.roundservice.controllers;

import com.dayo.careround.roundservice.dto.request.CreateRoundRequest;
import com.dayo.careround.roundservice.data.models.enums.RoundStatus;
import com.dayo.careround.roundservice.dto.response.PageResponse;
import com.dayo.careround.roundservice.dto.response.RoundPatientReviewResponse;
import com.dayo.careround.roundservice.dto.response.RoundResponse;
import com.dayo.careround.roundservice.exception.BadRequestException;
import com.dayo.careround.roundservice.grpc.ChargeVerificationClient;
import com.dayo.careround.roundservice.services.RoundPatientReviewService;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.List;
import com.dayo.careround.roundservice.security.CareRoundPrincipal;
import com.dayo.careround.roundservice.services.RoundLifecycleService;
import com.dayo.careround.roundservice.services.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rounds")
@RequiredArgsConstructor
public class RoundController {

    private final RoundService roundService;
    private final RoundLifecycleService roundLifecycleService;
    private final RoundPatientReviewService reviewService;
    private final ChargeVerificationClient chargeVerificationClient;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE')")
    public ResponseEntity<RoundResponse> create(
            @Valid @RequestBody CreateRoundRequest request,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roundService.create(request, principal.userId(), principal.role()));
    }

    // Paged round list, newest first (createdAt desc). doctorId = the round's conductedBy.
    // ADMIN: any filters. DOCTOR: own rounds only (doctorId defaults to the caller; another doctor's id -> 403).
    // NURSE: wardId required and must be the ward's active charge nurse (ward-service; 503 if unreachable).
    @GetMapping
    public ResponseEntity<PageResponse<RoundResponse>> list(
            @RequestParam(required = false) UUID wardId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) RoundStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        switch (principal.role()) {
            case "ADMIN" -> { }
            case "DOCTOR" -> {
                if (doctorId != null && !doctorId.equals(principal.userId())) {
                    throw new AccessDeniedException("Doctors can list only their own rounds");
                }
                doctorId = principal.userId();
            }
            case "NURSE" -> {
                if (wardId == null) throw new BadRequestException("wardId is required");
                if (!chargeVerificationClient.isActiveChargeNurse(wardId, principal.userId())) {
                    throw new AccessDeniedException("Only the ward's active charge nurse can list its rounds");
                }
            }
            default -> throw new AccessDeniedException("Not allowed to list rounds");
        }
        return ResponseEntity.ok(roundService.list(wardId, doctorId, status, from, to, page, size));
    }

    // The patients on a round: one review per patient (same data as GET /api/v1/reviews?roundPublicId=).
    @GetMapping("/{publicId}/patients")
    public ResponseEntity<List<RoundPatientReviewResponse>> patients(@PathVariable UUID publicId) {
        return ResponseEntity.ok(reviewService.getByRoundPublicId(publicId));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<RoundResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(roundService.getByPublicId(publicId));
    }

    @PostMapping("/{publicId}/end")
    public ResponseEntity<RoundResponse> endRound(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.ok(roundLifecycleService.endRound(publicId, principal.userId(), principal.role()));
    }
}