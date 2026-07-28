package com.dayo.careround.roundservice.data.repositories;

import com.dayo.careround.roundservice.data.models.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findByPatientId(UUID patientId);
}