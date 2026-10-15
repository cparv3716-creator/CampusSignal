package edu.campussignal.gmail;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import com.google.api.services.gmail.model.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GmailMessageMapperTest {
    private final GmailMessageMapper mapper = new GmailMessageMapper();

    private MessagePart part(String mime, String content) {
        return new MessagePart().setMimeType(mime).setBody(new MessagePartBody().setData(
                Base64.getUrlEncoder().withoutPadding().encodeToString(content.getBytes(StandardCharsets.UTF_8))));
    }

    private Message message(MessagePart payload) {
        return new Message().setId("gmail-123").setInternalDate(1788861600000L).setPayload(payload);
    }

    private MessagePartHeader header(String name, String value) {
        return new MessagePartHeader().setName(name).setValue(value);
    }

    @Test
    void decodesHeadersSenderBodyAndDate() {
        var payload = part("text/plain", "Campus café — आज").setHeaders(List.of(
                header("sUbJeCt", "=?UTF-8?B?Q2FtcHVzIGNhZsOp?="),
                header("From", "Notices <notices@example.edu>"),
                header("Date", "Tue, 8 Sep 2026 12:00:00 +0200")));
        var result = mapper.map(message(payload));
        assertThat(result.messageId()).isEqualTo("gmail-123");
        assertThat(result.subject()).isEqualTo("Campus café");
        assertThat(result.sender()).isEqualTo("notices@example.edu");
        assertThat(result.body()).isEqualTo("Campus café — आज");
        assertThat(result.receivedAt()).isEqualTo(Instant.parse("2026-09-08T10:00:00Z"));
    }

    @Test
    void nestedMultipartPrefersPlainTextAndIgnoresAttachments() {
        var payload = new MessagePart().setMimeType("multipart/mixed").setParts(List.of(
                new MessagePart().setMimeType("multipart/alternative").setParts(List.of(
                        part("text/html", "<p>HTML version</p>"), part("text/plain", "Plain version"))),
                part("text/plain", "Attachment content").setFilename("notes.txt")));
        assertThat(mapper.map(message(payload)).body()).isEqualTo("Plain version");
    }

    @Test
    void htmlFallbackRemovesScriptsStylesAndDecodesEntities() {
        var payload = part("text/html", "<style>hidden</style><p>Hello &amp; welcome</p><script>hidden()</script>");
        assertThat(mapper.map(message(payload)).body()).isEqualTo("Hello & welcome");
    }

    @Test
    void charsetIsRespected() {
        var payload = new MessagePart().setMimeType("text/plain")
                .setHeaders(List.of(header("Content-Type", "text/plain; charset=ISO-8859-1")))
                .setBody(new MessagePartBody().setData(Base64.getUrlEncoder().withoutPadding()
                        .encodeToString("café".getBytes(StandardCharsets.ISO_8859_1))));
        assertThat(mapper.map(message(payload)).body()).isEqualTo("café");
    }

    @Test
    void missingOptionalHeadersAndInvalidDateHaveSafeFallbacks() {
        var result = mapper.map(message(part("text/plain", "body")
                .setHeaders(List.of(header("Date", "invalid date")))));
        assertThat(result.subject()).isEqualTo("(no subject)");
        assertThat(result.sender()).isEqualTo("unknown");
        assertThat(result.receivedAt()).isEqualTo(Instant.ofEpochMilli(1788861600000L));
        assertThat(mapper.map(message(null)).body()).isEmpty();
    }

    @Test
    void rejectsMissingIdentityTimestampAndMalformedBody() {
        assertThatThrownBy(() -> mapper.map(message(null).setId(null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.map(message(null).setInternalDate(null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.map(message(new MessagePart().setMimeType("text/plain")
                .setBody(new MessagePartBody().setData("***"))))).isInstanceOf(IllegalArgumentException.class);
    }
}
