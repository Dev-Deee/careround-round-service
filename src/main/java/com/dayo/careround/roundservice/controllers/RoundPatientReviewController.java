package com.dayo.careround.roundservice.controllers;

import com.dayo.careround.roundservice.dto.request.ResolveReviewRequest;
import com.dayo.careround.roundservice.dto.response.RoundPatientReviewResponse;
import com.dayo.careround.roundservice.security.CareRoundPrincipal;
import com.dayo.careround.roundservice.services.RoundPatientReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class RoundPatientReviewController {

    private final RoundPatientReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<RoundPatientReviewResponse>> getByRound(@RequestParam UUID roundPublicId) {
        return ResponseEntity.ok(reviewService.getByRoundPublicId(roundPublicId));
    }

    @PostMapping("/{reviewId}/acknowledge-tasks")
    public ResponseEntity<Void> acknowledgeTasks(@PathVariable UUID reviewId) {
        reviewService.acknowledgeTasks(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reviewId}/resolve")
    public ResponseEntity<RoundPatientReviewResponse> resolve(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ResolveReviewRequest request,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.ok(reviewService.resolve(reviewId, principal.userId(), request));
    }
}