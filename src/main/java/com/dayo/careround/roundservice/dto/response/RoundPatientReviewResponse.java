package com.dayo.careround.roundservice.dto.response;

import com.dayo.careround.roundservice.data.models.enums.RoundPatientReviewState;

import java.time.Instant;
import java.util.UUID;

public record RoundPatientReviewResponse(
        UUID id,
        UUID roundId, // deprecated: the round's internal id; use roundPublicId
        UUID patientId,
        RoundPatientReviewState state,
        boolean tasksAcknowledged,
        UUID clinicalNoteId,
        String statusSummary,
        UUID resolvedByDoctorId,
        Instant reviewedAt,
        UUID roundPublicId
) {}