package edu.campussignal.dto;

import java.time.Instant;

public record EmailResponse(
        Long id,
        String subject,
        String sender,
        Instant receivedAt,
        EmailCategory category,
        double classificationConfidence) {}
