package edu.campussignal.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GmailNotificationDecoderTest {
    private final GmailNotificationDecoder decoder = new GmailNotificationDecoder(new ObjectMapper());

    @Test
    void acceptsStringAndNumericHistoryIds() {
        assertThat(decoder.decode("{\"emailAddress\":\" student@example.edu \",\"historyId\":\"123\"}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isEqualTo(new GmailNotification("student@example.edu", "123"));
        assertThat(decoder.decode("{\"emailAddress\":\"student@example.edu\",\"historyId\":124}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).historyId()).isEqualTo("124");
    }

    @Test
    void rejectsMalformedOrIncompleteNotifications() {
        assertThatThrownBy(() -> decoder.decode("not-json".getBytes()))
                .isInstanceOf(InvalidGmailNotificationException.class);
        assertThatThrownBy(() -> decoder.decode("[]".getBytes()))
                .isInstanceOf(InvalidGmailNotificationException.class);
        assertThatThrownBy(() -> decoder.decode("{\"emailAddress\":\"\",\"historyId\":1}".getBytes()))
                .isInstanceOf(InvalidGmailNotificationException.class);
        assertThatThrownBy(() -> decoder.decode("{\"emailAddress\":\"a@b.com\",\"historyId\":true}".getBytes()))
                .isInstanceOf(InvalidGmailNotificationException.class);
        assertThatThrownBy(() -> decoder.decode("{\"emailAddress\":\"a@b.com\",\"historyId\":-1}".getBytes()))
                .isInstanceOf(InvalidGmailNotificationException.class);
    }
}
