package edu.campussignal.gmail;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.WatchRequest;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.service.EmailSource;

/** One authorized local mailbox. Google types do not cross the EmailSource boundary. */
public class GmailApiClient implements EmailSource {
    public record WatchRegistration(String historyId, Instant expiresAt) {}

    private final Gmail gmail;
    private final String userId;
    private final GmailMessageMapper mapper = new GmailMessageMapper();

    public GmailApiClient(Gmail gmail) {
        this(gmail, "me");
    }

    public GmailApiClient(Gmail gmail, String userId) {
        this.gmail = gmail;
        this.userId = userId == null || userId.isBlank() ? "me" : userId.strip();
    }

    @Override
    public List<String> listMessageIds(int maxResults) throws IOException {
        if (maxResults < 1 || maxResults > 500) {
            throw new IllegalArgumentException("maxResults must be between 1 and 500");
        }
        var ids = new LinkedHashSet<String>();
        var seenTokens = new HashSet<String>();
        String pageToken = null;
        do {
            var page = gmail.users().messages().list(userId)
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
        var message = gmail.users().messages().get(userId, messageId).setFormat("full").execute();
        if (!messageId.equals(message.getId())) {
            throw new IOException("Gmail returned a different message ID");
        }
        return mapper.map(message);
    }

    public WatchRegistration watchInbox(String topicName) throws IOException {
        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException("A Pub/Sub topic is required");
        }
        var request = new WatchRequest()
                .setTopicName(topicName.strip())
                .setLabelIds(List.of("INBOX"))
                .setLabelFilterBehavior("include");
        var response = gmail.users().watch(userId, request).execute();
        if (response.getHistoryId() == null || response.getHistoryId().signum() < 0
                || response.getExpiration() == null || response.getExpiration() < 0) {
            throw new IOException("Gmail watch returned an invalid response");
        }
        return new WatchRegistration(response.getHistoryId().toString(),
                Instant.ofEpochMilli(response.getExpiration()));
    }

    public ListHistoryResponse listHistoryPage(String startHistoryId, String pageToken)
            throws IOException {
        var start = historyId(startHistoryId);
        try {
            var request = gmail.users().history().list(userId)
                    .setStartHistoryId(start)
                    .setHistoryTypes(List.of("messageAdded"))
                    .setLabelId("INBOX")
                    .setMaxResults(500L);
            if (pageToken != null && !pageToken.isBlank()) {
                request.setPageToken(pageToken);
            }
            return request.execute();
        } catch (GoogleJsonResponseException exception) {
            if (exception.getStatusCode() == 404) {
                throw new StaleGmailHistoryException();
            }
            throw exception;
        }
    }

    public String currentHistoryId() throws IOException {
        var profile = gmail.users().getProfile(userId).execute();
        if (profile.getHistoryId() == null || profile.getHistoryId().signum() < 0) {
            throw new IOException("Gmail profile returned no usable history ID");
        }
        return profile.getHistoryId().toString();
    }

    static BigInteger historyId(String value) {
        try {
            var parsed = new BigInteger(value);
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException("Gmail history ID must not be negative");
            }
            return parsed;
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException("Gmail history ID must be an integer string", exception);
        }
    }
}
