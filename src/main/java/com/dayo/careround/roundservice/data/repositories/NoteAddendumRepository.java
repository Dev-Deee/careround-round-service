package com.dayo.careround.roundservice.data.repositories;

import com.dayo.careround.roundservice.data.models.NoteAddendum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteAddendumRepository extends JpaRepository<NoteAddendum, UUID> {
    List<NoteAddendum> findByNoteId(UUID noteId);
}