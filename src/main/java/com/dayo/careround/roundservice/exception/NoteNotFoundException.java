package com.dayo.careround.roundservice.exception;

public class NoteNotFoundException extends RuntimeException{
    public NoteNotFoundException(String message){
        super(message);
    }
}
