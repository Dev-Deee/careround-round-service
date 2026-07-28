package com.dayo.careround.roundservice.data.models;

import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
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

    @Column
    private UUID clinicalNoteId;

    @Column
    private String statusSummary; // e.g. "Improving" | "Stable" | "Needs attention" | "Critical"

    @Column
    private UUID resolvedByDoctorId;

    @Column
    private Instant reviewedAt;
}