package com.dayo.careround.roundservice.data.models;

import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "round_patient_reviews")
public class RoundPatientReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID roundId;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundPatientReviewState state = RoundPatientReviewState.PENDING;

    @Column(nullable = false)
    private boolean tasksAcknowledged = false;

    // Care-task public ids that were pending when tasks were last acknowledged (FR-1.3a).
    // Resolve is blocked while any currently pending task is missing from this set.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "round_patient_review_acknowledged_tasks", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "care_task_id", nullable = false)
    private Set<UUID> acknowledgedTaskIds = new HashSet<>();

    @Column
    private UUID clinicalNoteId;

    @Column
    private String statusSummary; // e.g. "Improving" | "Stable" | "Needs attention" | "Critical"

    @Column
    private UUID resolvedByDoctorId;

    @Column
    private Instant reviewedAt;
}