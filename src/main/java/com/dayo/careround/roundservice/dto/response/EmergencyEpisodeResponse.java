package com.dayo.careround.roundservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EmergencyEpisodeResponse(
        UUID id,
        UUID patientId,
        UUID declaredBy,
        Instant declaredAt,
        Instant resolvedAt
) {}