package com.dayo.careround.roundservice.exception;

public class EmergencyAlreadyActiveException extends RuntimeException {
    public EmergencyAlreadyActiveException(String message) {
        super(message);
    }
}
