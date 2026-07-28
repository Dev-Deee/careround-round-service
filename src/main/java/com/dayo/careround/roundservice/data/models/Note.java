package com.dayo.careround.roundservice.data.models;

import com.dayo.careround.roundservice.data.models.enums.NoteContext;
import com.dayo.careround.roundservice.data.models.enums.NoteStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteContext context;

    @Column(nullable = false)
    private UUID authorId;

    @Column
    private UUID roundId; // nullable — EMERGENCY/DISCHARGE notes may not belong to a round

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteStatus status = NoteStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean isCarriedForward = false;

    @Column
    private UUID carryForwardSourceNoteId;
}