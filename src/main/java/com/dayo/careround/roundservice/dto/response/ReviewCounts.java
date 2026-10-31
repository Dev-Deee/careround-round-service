package com.dayo.careround.roundservice.dto.response;

// Reviews under a round by state. CARRIED_FORWARD and DISCHARGED reviews are not counted here.
public record ReviewCounts(long pending, long suspended, long resolved) {}
