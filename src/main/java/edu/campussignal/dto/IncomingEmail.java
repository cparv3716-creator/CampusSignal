package edu.campussignal.dto;

import java.time.Instant;
import java.util.Objects;

/** Provider-neutral data accepted by persistence; messageId is the provider's stable ID. */
public record IncomingEmail(String messageId, String subject, String sender,
                            Instant receivedAt, String body) {
    public IncomingEmail {
        if (messageId == null || messageId.isBlank() || messageId.length() > 255) {
            throw new IllegalArgumentException("A message ID of at most 255 characters is required");
        }
        Objects.requireNonNull(subject);
        Objects.requireNonNull(sender);
        Objects.requireNonNull(receivedAt);
        Objects.requireNonNull(body);
    }
}
