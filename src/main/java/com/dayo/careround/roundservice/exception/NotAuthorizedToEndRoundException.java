package com.dayo.careround.roundservice.exception;

public class NotAuthorizedToEndRoundException extends RuntimeException {
    public NotAuthorizedToEndRoundException(String message) {
        super(message);
    }
}
