package edu.campussignal.dto;

import java.net.URI;
import java.time.LocalDate;
import java.util.Locale;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailActionCreateRequest(
        @NotNull EmailActionType actionType,
        LocalDate deadline,
        @Size(max = 2048) String actionUrl) {

    public EmailActionCreateRequest {
        actionUrl = normalizeUrl(actionUrl);
    }

    private static String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        URI uri = URI.create(normalized);
        String scheme = uri.getScheme();
        String normalizedScheme = scheme == null ? null : scheme.toLowerCase(Locale.ROOT);
        if (scheme == null
                || !("http".equals(normalizedScheme) || "https".equals(normalizedScheme))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Action URL must be an absolute HTTP or HTTPS URL");
        }
        return normalized;
    }
}
