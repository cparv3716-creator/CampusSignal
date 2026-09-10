package edu.campussignal.gmail;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class GmailNotificationDecoder {
    private final ObjectMapper mapper;

    public GmailNotificationDecoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public GmailNotification decode(byte[] data) {
        final JsonNode root;
        try {
            root = mapper.readTree(data);
        } catch (IOException exception) {
            throw new InvalidGmailNotificationException("Gmail notification is not valid JSON", exception);
        }
        if (root == null || !root.isObject()) {
            throw new InvalidGmailNotificationException("Gmail notification is not a JSON object");
        }
        var email = root.get("emailAddress");
        if (email == null || !email.isTextual() || email.textValue().isBlank()) {
            throw new InvalidGmailNotificationException("Gmail notification is missing emailAddress");
        }
        var history = root.get("historyId");
        String historyId;
        if (history != null && history.isIntegralNumber()) {
            var number = history.bigIntegerValue();
            if (number.signum() < 0) {
                throw new InvalidGmailNotificationException("Gmail notification has an invalid historyId");
            }
            historyId = number.toString();
        } else if (history != null && history.isTextual()
                && history.textValue().matches("[0-9]+")) {
            historyId = history.textValue();
        } else {
            throw new InvalidGmailNotificationException("Gmail notification has an invalid historyId");
        }
        return new GmailNotification(email.textValue().strip(), historyId);
    }
}
