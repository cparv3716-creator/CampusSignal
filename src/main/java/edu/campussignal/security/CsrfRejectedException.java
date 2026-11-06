package edu.campussignal.security;
public class CsrfRejectedException extends RuntimeException { public CsrfRejectedException() { super("CSRF validation failed"); } }
