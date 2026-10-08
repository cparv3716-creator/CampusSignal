package edu.campussignal.service;

public class EmailActionNotFoundException extends RuntimeException {
    public EmailActionNotFoundException() {
        super("Action not found");
    }
}
