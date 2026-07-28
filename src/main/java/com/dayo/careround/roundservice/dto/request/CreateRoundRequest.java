package com.dayo.careround.roundservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateRoundRequest(
        @NotNull UUID wardId,
        @NotNull List<UUID> patientIds
) {}