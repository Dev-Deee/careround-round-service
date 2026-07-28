package com.dayo.careround.roundservice.dto.response;

import com.dayo.careround.roundservice.data.models.enums.NoteContext;
import com.dayo.careround.roundservice.data.models.enums.NoteStatus;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        UUID patientId,
        NoteContext context,
        UUID authorId,
        UUID roundId,
        String content,
        NoteStatus status,
        Instant createdAt,
        boolean isCarriedForward
) {}