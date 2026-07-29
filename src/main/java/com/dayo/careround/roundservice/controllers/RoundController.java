package com.dayo.careround.roundservice.controllers;

import com.dayo.careround.roundservice.dto.request.CreateRoundRequest;
import com.dayo.careround.roundservice.dto.response.RoundResponse;
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

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE')")
    public ResponseEntity<RoundResponse> create(@Valid @RequestBody CreateRoundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roundService.create(request));
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