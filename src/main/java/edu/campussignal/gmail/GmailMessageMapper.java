package edu.campussignal.gmail;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MailDateFormat;
import jakarta.mail.internet.MimeUtility;
import org.jsoup.Jsoup;
import edu.campussignal.dto.IncomingEmail;

public class GmailMessageMapper {
    public IncomingEmail map(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Gmail returned no message");
        }
        MessagePart payload = message.getPayload();
        String subject = decodeHeader(header(payload, "Subject"));
        String sender = sender(header(payload, "From"));
        List<String> plain = new ArrayList<>();
        List<String> html = new ArrayList<>();
        collectText(payload, plain, html);
        String body = plain.isEmpty() ? String.join("\n\n", html) : String.join("\n\n", plain);
        return new IncomingEmail(message.getId(), subject.isBlank() ? "(no subject)" : subject,
                sender, receivedAt(message), body.strip());
    }

    private String header(MessagePart part, String name) {
        if (part == null || part.getHeaders() == null) {
            return "";
        }
        return part.getHeaders().stream().filter(value -> name.equalsIgnoreCase(value.getName()))
                .map(value -> value.getValue() == null ? "" : value.getValue()).findFirst().orElse("");
    }

    private String decodeHeader(String value) {
        try {
            return MimeUtility.decodeText(value).strip();
        } catch (java.io.UnsupportedEncodingException exception) {
            return value.strip();
        }
    }

    private String sender(String value) {
        try {
            var addresses = InternetAddress.parse(value);
            if (addresses.length > 0 && addresses[0].getAddress() != null) {
                return addresses[0].getAddress();
            }
        } catch (jakarta.mail.internet.AddressException ignored) {
            // A malformed optional header must not discard the entire message.
        }
        return "unknown";
    }

    private Instant receivedAt(Message message) {
        String date = header(message.getPayload(), "Date");
        if (!date.isBlank()) {
            try {
                return new MailDateFormat().parse(date).toInstant();
            } catch (java.text.ParseException ignored) {
                // Match the prototype: prefer a valid Date header, then Gmail's epoch milliseconds.
            }
        }
        if (message.getInternalDate() == null) {
            throw new IllegalArgumentException("Gmail message has no usable timestamp");
        }
        return Instant.ofEpochMilli(message.getInternalDate());
    }

    private void collectText(MessagePart part, List<String> plain, List<String> html) {
        if (part == null || (part.getFilename() != null && !part.getFilename().isBlank())
                || header(part, "Content-Disposition").toLowerCase(Locale.ROOT).startsWith("attachment")) {
            return;
        }
        String mime = part.getMimeType() == null ? "" : part.getMimeType().toLowerCase(Locale.ROOT);
        if (part.getBody() != null && part.getBody().getData() != null
                && ("text/plain".equals(mime) || "text/html".equals(mime))) {
            byte[] data = Base64.getUrlDecoder().decode(part.getBody().getData());
            String decoded = new String(data, charset(part));
            if ("text/plain".equals(mime)) {
                plain.add(decoded);
            } else {
                var document = Jsoup.parse(decoded);
                document.select("script,style").remove();
                html.add(document.text());
            }
        }
        if (part.getParts() != null) {
            part.getParts().forEach(child -> collectText(child, plain, html));
        }
    }

    private Charset charset(MessagePart part) {
        try {
            String charset = new ContentType(header(part, "Content-Type")).getParameter("charset");
            return charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
        } catch (jakarta.mail.internet.ParseException | IllegalArgumentException exception) {
            return StandardCharsets.UTF_8;
        }
    }
}
