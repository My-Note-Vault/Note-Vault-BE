package com.example.workspace.workspace.command.application;

public class AlreadyInWorkSpaceException extends RuntimeException {
    public AlreadyInWorkSpaceException(String message) {
        super(message);
    }
}
