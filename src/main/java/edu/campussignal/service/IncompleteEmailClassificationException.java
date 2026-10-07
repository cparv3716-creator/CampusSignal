package edu.campussignal.service;

public class IncompleteEmailClassificationException extends RuntimeException {
    public IncompleteEmailClassificationException() {
        super("Stored email classification is incomplete");
    }
}
