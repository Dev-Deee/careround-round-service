package com.dayo.careround.roundservice.controllers;

import com.dayo.careround.roundservice.dto.request.AddNoteAddendumRequest;
import com.dayo.careround.roundservice.dto.request.CreateNoteRequest;
import com.dayo.careround.roundservice.dto.request.UpdateNoteContentRequest;
import com.dayo.careround.roundservice.dto.response.NoteAddendumResponse;
import com.dayo.careround.roundservice.dto.response.NoteResponse;
import com.dayo.careround.roundservice.security.CareRoundPrincipal;
import com.dayo.careround.roundservice.services.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<NoteResponse> create(
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(principal.userId(), request));
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<NoteResponse> updateDraft(
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteContentRequest request,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.ok(noteService.updateDraft(noteId, principal.userId(), request));
    }

    @PostMapping("/{noteId}/commit")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<NoteResponse> commit(
            @PathVariable UUID noteId,
            @RequestParam(required = false) UUID reviewId,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.ok(noteService.commit(noteId, principal.userId(), reviewId));
    }

    @PostMapping("/{noteId}/addenda")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<NoteAddendumResponse> addAddendum(
            @PathVariable UUID noteId,
            @Valid @RequestBody AddNoteAddendumRequest request,
            @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.addAddendum(noteId, principal.userId(), request));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getByPatient(@RequestParam UUID patientId,
                                                           @AuthenticationPrincipal CareRoundPrincipal principal) {
        return ResponseEntity.ok(noteService.getByPatient(patientId, principal.userId()));
    }

    @GetMapping("/{noteId}/addenda")
    public ResponseEntity<List<NoteAddendumResponse>> getAddenda(@PathVariable UUID noteId) {
        return ResponseEntity.ok(noteService.getAddenda(noteId));
    }
}