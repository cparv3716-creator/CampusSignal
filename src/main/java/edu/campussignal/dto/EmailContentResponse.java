package edu.campussignal.dto;

import java.time.Instant;

public record EmailContentResponse(
        Long id,
        String subject,
        String sender,
        Instant receivedAt,
        String body,
        boolean truncated) {}
