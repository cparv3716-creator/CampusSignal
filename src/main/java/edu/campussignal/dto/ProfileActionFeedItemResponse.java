package edu.campussignal.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ProfileActionFeedItemResponse(
        Long id,
        Long emailId,
        String emailSubject,
        EmailCategory emailCategory,
        ClassificationConfidence classificationConfidence,
        EmailActionType actionType,
        LocalDate deadline,
        String actionUrl,
        EmailActionEffectiveStatus effectiveStatus,
        ActionUrgency urgency,
        Integer daysRemaining,
        ActionRelevance relevance,
        int relevanceScore,
        int rankingScore,
        String rankingExplanation,
        Instant createdAt) {}
