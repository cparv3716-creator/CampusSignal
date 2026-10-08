package edu.campussignal.dto;

import java.time.Instant;
import java.time.LocalDate;

public record EmailActionReadResponse(
        Long id,
        Long emailId,
        String emailSubject,
        EmailActionType actionType,
        EmailActionStatus status,
        EmailActionEffectiveStatus effectiveStatus,
        ActionUrgency urgency,
        Integer daysRemaining,
        LocalDate deadline,
        String actionUrl,
        Instant completedAt,
        Instant createdAt) {}
