package com.dayo.careround.roundservice.data.models.enums;

public enum OutboxStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    DEAD_LETTER
}