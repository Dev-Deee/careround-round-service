package com.dayo.careround.roundservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeclareEmergencyRequest(
        @NotNull UUID patientId,
        @NotBlank String idempotencyKey
) {}