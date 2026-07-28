package com.dayo.careround.roundservice.dto.request;

import com.dayo.careround.roundservice.data.models.enums.NoteContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateNoteRequest(
        @NotNull UUID patientId,
        @NotNull NoteContext context,
        UUID roundId, // nullable — only required for context = ROUND
        @NotBlank String content
) {}