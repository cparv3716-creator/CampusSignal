package edu.campussignal.dto;
public record SessionResponse(String email, String name, boolean profileComplete, boolean gmailConnected, String csrfToken) { }
