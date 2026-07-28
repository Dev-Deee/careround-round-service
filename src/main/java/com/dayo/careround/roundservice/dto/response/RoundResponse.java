package com.dayo.careround.roundservice.dto.response;

import com.dayo.careround.roundservice.data.models.enums.RoundStatus;

import java.util.UUID;

public record RoundResponse(
        UUID publicId,
        UUID wardId,
        RoundStatus status
) {}