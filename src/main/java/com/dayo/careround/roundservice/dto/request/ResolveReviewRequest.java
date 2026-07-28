package com.dayo.careround.roundservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResolveReviewRequest(
        @NotBlank String statusSummary
) {}