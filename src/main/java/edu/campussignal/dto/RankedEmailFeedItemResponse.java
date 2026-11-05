package edu.campussignal.dto;

import java.time.Instant;
import java.time.LocalDate;

public record RankedEmailFeedItemResponse(
        Long emailId,
        String emailSubject,
        EmailCategory emailCategory,
        ClassificationConfidence classificationConfidence,
        int relevanceScore,
        int rankingScore,
        String rankingExplanation,
        LocalDate deadline,
        EmailActionType actionType,
        String actionUrl,
        ActionUrgency urgency,
        Integer daysRemaining,
        boolean actionIndicated,
        boolean trackedAction,
        EmailActionStatus trackedActionStatus,
        Instant receivedAt) {}
