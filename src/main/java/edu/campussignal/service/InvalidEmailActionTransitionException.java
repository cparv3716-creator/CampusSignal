package edu.campussignal.service;

public class InvalidEmailActionTransitionException extends RuntimeException {
    public InvalidEmailActionTransitionException() {
        super("Action status transition is not allowed");
    }
}
