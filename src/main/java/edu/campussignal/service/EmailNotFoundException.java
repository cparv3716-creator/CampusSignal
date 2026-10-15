package edu.campussignal.service;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException() { super("Email not found"); }
}
