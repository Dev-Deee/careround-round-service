package com.dayo.careround.roundservice.exception;

public class OutboxEventNotFoundException extends RuntimeException {
    public OutboxEventNotFoundException(String message) {
        super(message);
    }
}
