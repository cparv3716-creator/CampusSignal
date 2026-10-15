package edu.campussignal.service;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() { super("Profile not found"); }
}
