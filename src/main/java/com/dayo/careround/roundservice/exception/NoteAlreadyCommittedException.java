package com.dayo.careround.roundservice.exception;

public class NoteAlreadyCommittedException extends RuntimeException {
    public NoteAlreadyCommittedException(String message){
        super(message);
    }
}
