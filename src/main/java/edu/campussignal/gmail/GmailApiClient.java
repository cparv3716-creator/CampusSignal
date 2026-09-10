package edu.campussignal.gmail;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import com.google.api.services.gmail.Gmail;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.service.EmailSource;

/** One authorized local mailbox. Google types do not cross the EmailSource boundary. */
public class GmailApiClient implements EmailSource {
    private final Gmail gmail;
    private final GmailMessageMapper mapper = new GmailMessageMapper();

    public GmailApiClient(Gmail gmail) { this.gmail = gmail; }

    @Override
    public List<String> listMessageIds(int maxResults) throws IOException {
        if (maxResults < 1 || maxResults > 500) {
            throw new IllegalArgumentException("maxResults must be between 1 and 500");
        }
        var ids = new LinkedHashSet<String>();
        var seenTokens = new HashSet<String>();
        String pageToken = null;
        do {
            var page = gmail.users().messages().list("me")
                    .setLabelIds(List.of("INBOX"))
                    .setMaxResults((long) (maxResults - ids.size()))
                    .setPageToken(pageToken).execute();
            if (page.getMessages() != null) {
                for (var message : page.getMessages()) {
                    if (message.getId() != null && !message.getId().isBlank()) {
                        ids.add(message.getId());
                    }
                    if (ids.size() == maxResults) {
                        break;
                    }
                }
            }
            pageToken = page.getNextPageToken();
            if (pageToken != null && !seenTokens.add(pageToken)) {
                throw new IOException("Gmail returned a repeated page token");
            }
        } while (pageToken != null && !pageToken.isBlank() && ids.size() < maxResults);
        return List.copyOf(ids);
    }

    @Override
    public IncomingEmail fetch(String messageId) throws IOException {
        var message = gmail.users().messages().get("me", messageId).setFormat("full").execute();
        if (!messageId.equals(message.getId())) {
            throw new IOException("Gmail returned a different message ID");
        }
        return mapper.map(message);
    }
}
