package com.dayo.careround.roundservice.controllers;

import com.dayo.careround.roundservice.dto.request.DeclareEmergencyRequest;
import com.dayo.careround.roundservice.dto.response.EmergencyEpisodeResponse;
import com.dayo.careround.roundservice.security.CareRoundPrincipal;
import com.dayo.careround.roundservice.services.EmergencyEpisodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergencies")
@RequiredArgsConstructor
public class EmergencyEpisodeController {

    private final EmergencyEpisodeService emergencyEpisodeService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE')")
    public ResponseEntity<EmergencyEpisodeResponse> declare(
            @Valid @RequestBody DeclareEmergencyRequest request,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emergencyEpisodeService.declare(principal.userId(), request));
    }

    @PostMapping("/{episodeId}/resolve")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE')")
    public ResponseEntity<EmergencyEpisodeResponse> resolve(
            @PathVariable UUID episodeId,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.ok(emergencyEpisodeService.resolve(episodeId, principal.userId()));
    }

    @GetMapping("/active")
    public ResponseEntity<EmergencyEpisodeResponse> getActive(@RequestParam UUID patientId) {
        return ResponseEntity.ok(emergencyEpisodeService.getActiveForPatient(patientId));
    }
}