package com.dayo.careround.roundservice.services;

import com.dayo.careround.roundservice.data.models.Note;
import com.dayo.careround.roundservice.data.models.NoteAddendum;
import com.dayo.careround.roundservice.data.models.enums.NoteStatus;
import com.dayo.careround.roundservice.data.repositories.NoteAddendumRepository;
import com.dayo.careround.roundservice.data.repositories.NoteRepository;
import com.dayo.careround.roundservice.dto.request.AddNoteAddendumRequest;
import com.dayo.careround.roundservice.dto.request.CreateNoteRequest;
import com.dayo.careround.roundservice.dto.request.UpdateNoteContentRequest;
import com.dayo.careround.roundservice.dto.response.NoteAddendumResponse;
import com.dayo.careround.roundservice.dto.response.NoteResponse;
import com.dayo.careround.roundservice.exception.NoteAlreadyCommittedException;
import com.dayo.careround.roundservice.exception.NoteNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteAddendumRepository noteAddendumRepository;
    private final RoundPatientReviewService reviewService;

    @Transactional
    public NoteResponse create(UUID authorId, CreateNoteRequest request) {
        Note note = new Note();
        note.setPatientId(request.patientId());
        note.setContext(request.context());
        note.setAuthorId(authorId);
        note.setRoundId(request.roundId());
        note.setContent(request.content());
        note.setStatus(NoteStatus.DRAFT);

        noteRepository.save(note);
        return toResponse(note);
    }

    @Transactional
    public NoteResponse updateDraft(UUID noteId, UpdateNoteContentRequest request) {
        Note note = findOrThrow(noteId);

        if (note.getStatus() == NoteStatus.COMMITTED) {
            throw new NoteAlreadyCommittedException("A committed note cannot be edited — add an addendum instead");
        }

        note.setContent(request.content());
        noteRepository.save(note);
        return toResponse(note);
    }

    /**
     * Commits the note (making it immutable) and, if it belongs to a round review,
     * attaches it as that review's clinicalNoteId — one of the three RESOLVED gates.
     */
    @Transactional
    public NoteResponse commit(UUID noteId, UUID reviewId) {
        Note note = findOrThrow(noteId);

        if (note.getStatus() == NoteStatus.COMMITTED) {
            throw new NoteAlreadyCommittedException("This note is already committed");
        }

        note.setStatus(NoteStatus.COMMITTED);
        noteRepository.save(note);

        if (reviewId != null) {
            reviewService.attachNote(reviewId, note.getId());
        }

        return toResponse(note);
    }

    @Transactional
    public NoteAddendumResponse addAddendum(UUID noteId, UUID authorId, AddNoteAddendumRequest request) {
        Note note = findOrThrow(noteId);

        if (note.getStatus() != NoteStatus.COMMITTED) {
            throw new NoteAlreadyCommittedException("Addenda can only be added to a committed note");
        }

        NoteAddendum addendum = new NoteAddendum();
        addendum.setNoteId(noteId);
        addendum.setAuthorId(authorId);
        addendum.setContent(request.content());
        noteAddendumRepository.save(addendum);

        return toAddendumResponse(addendum);
    }

    public List<NoteResponse> getByPatient(UUID patientId) {
        return noteRepository.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NoteAddendumResponse> getAddenda(UUID noteId) {
        return noteAddendumRepository.findByNoteId(noteId).stream()
                .map(this::toAddendumResponse)
                .toList();
    }

    private Note findOrThrow(UUID noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException("No note with that id"));
    }

    private NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId(), note.getPatientId(), note.getContext(), note.getAuthorId(),
                note.getRoundId(), note.getContent(), note.getStatus(),
                note.getCreatedAt(), note.isCarriedForward()
        );
    }

    private NoteAddendumResponse toAddendumResponse(NoteAddendum addendum) {
        return new NoteAddendumResponse(
                addendum.getId(), addendum.getNoteId(), addendum.getAuthorId(),
                addendum.getContent(), addendum.getCreatedAt()
        );
    }
}