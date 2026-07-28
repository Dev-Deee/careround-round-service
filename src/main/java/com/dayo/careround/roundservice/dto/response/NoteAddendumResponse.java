package com.dayo.careround.roundservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NoteAddendumResponse(
        UUID id,
        UUID noteId,
        UUID authorId,
        String content,
        Instant createdAt
) {}