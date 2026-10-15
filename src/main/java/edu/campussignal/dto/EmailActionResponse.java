package edu.campussignal.dto;

import java.time.Instant;
import java.time.LocalDate;

public record EmailActionResponse(
        Long id,
        Long emailId,
        String emailSubject,
        EmailActionType actionType,
        EmailActionStatus status,
        LocalDate deadline,
        String actionUrl,
        Instant completedAt,
        Instant createdAt) {}
