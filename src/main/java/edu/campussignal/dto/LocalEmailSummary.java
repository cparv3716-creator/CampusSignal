package edu.campussignal.dto;

import java.util.List;

public record LocalEmailSummary(
        List<String> summary,
        List<String> importantDetails,
        String actionRequired,
        String deadline,
        String eligibility,
        List<String> evidence,
        boolean aiUsed) {
    public LocalEmailSummary {
        summary = List.copyOf(summary);
        importantDetails = List.copyOf(importantDetails);
        evidence = List.copyOf(evidence);
    }

    public static LocalEmailSummary fallback(String subject, String body) {
        String detail = body == null || body.isBlank()
                ? "The message does not contain enough text for a local summary."
                : "A deterministic summary is available from the stored message.";
        return new LocalEmailSummary(
                List.of(subject == null || subject.isBlank() ? "Email received" : subject.strip()),
                List.of(detail),
                null,
                null,
                null,
                List.of(),
                false);
    }
}
