package com.dayo.careround.roundservice.exception;

public class RoundNotFoundException extends RuntimeException {
    public RoundNotFoundException(String message) {
        super(message);
    }
}
