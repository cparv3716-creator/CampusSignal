package edu.campussignal.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import com.google.api.services.gmail.model.ListHistoryResponse;
import edu.campussignal.gmail.GmailApiClient;
import edu.campussignal.gmail.StaleGmailHistoryException;

@Service
public class GmailHistorySyncService {
    public record Summary(int historyRecords, int messagesFound, int created,
                          int alreadyExisted, boolean recoveredFromStaleHistory) {}

    private final EmailIngestionService ingestion;
    private final EmailRetrievalService retrieval;
    private final GmailWatchStateService state;

    public GmailHistorySyncService(EmailIngestionService ingestion, EmailRetrievalService retrieval,
                                   GmailWatchStateService state) {
        this.ingestion = ingestion;
        this.retrieval = retrieval;
        this.state = state;
    }

    /** Process one notification serially; Pub/Sub redelivery is safe because email IDs are unique. */
    public synchronized Summary sync(GmailApiClient client, String notificationHistoryId,
                                     int recoveryMaxResults) throws IOException {
        var stored = state.get().orElseThrow(() ->
                new IllegalStateException("Register Gmail watch before starting the listener"));
        var notification = historyId(notificationHistoryId);
        if (notification.compareTo(historyId(stored.lastHistoryId())) <= 0) {
            return new Summary(0, 0, 0, 0, false);
        }

        List<ListHistoryResponse> pages;
        try {
            pages = loadAllPages(client, stored.lastHistoryId());
        } catch (StaleGmailHistoryException exception) {
            return recoverFromStaleHistory(client, recoveryMaxResults);
        }

        var messageIds = messageAddedIds(pages);
        int created = 0;
        int existing = 0;
        for (String messageId : messageIds) {
            var result = ingestion.ingest(client.fetch(messageId));
            if (result.created()) {
                created++;
            } else {
                existing++;
            }
        }

        String newestHistoryId = notificationHistoryId;
        for (var page : pages) {
            if (page.getHistoryId() != null) {
                newestHistoryId = page.getHistoryId().toString();
            }
        }
        state.advance(newestHistoryId);
        int historyRecords = pages.stream()
                .mapToInt(page -> page.getHistory() == null ? 0 : page.getHistory().size()).sum();
        return new Summary(historyRecords, messageIds.size(), created, existing, false);
    }

    private List<ListHistoryResponse> loadAllPages(GmailApiClient client, String startHistoryId)
            throws IOException {
        var pages = new ArrayList<ListHistoryResponse>();
        var seenTokens = new HashSet<String>();
        String pageToken = null;
        do {
            var page = client.listHistoryPage(startHistoryId, pageToken);
            pages.add(page);
            pageToken = page.getNextPageToken();
            if (pageToken != null && !pageToken.isBlank() && !seenTokens.add(pageToken)) {
                throw new IOException("Gmail history pagination repeated a page token");
            }
        } while (pageToken != null && !pageToken.isBlank());
        return pages;
    }

    private Summary recoverFromStaleHistory(GmailApiClient client, int recoveryMaxResults)
            throws IOException {
        var recovery = retrieval.retrieve(client, recoveryMaxResults);
        if (recovery.failed() != 0) {
            throw new IOException("Bounded Gmail recovery synchronization failed");
        }
        state.advance(client.currentHistoryId());
        return new Summary(0, recovery.fetched(), recovery.created(),
                recovery.alreadyExisted(), true);
    }

    private static List<String> messageAddedIds(List<ListHistoryResponse> pages) {
        var ids = new LinkedHashSet<String>();
        for (var page : pages) {
            if (page.getHistory() == null) {
                continue;
            }
            for (var record : page.getHistory()) {
                if (record.getMessagesAdded() == null) {
                    continue;
                }
                for (var addition : record.getMessagesAdded()) {
                    var message = addition.getMessage();
                    if (message != null && message.getId() != null && !message.getId().isBlank()) {
                        ids.add(message.getId());
                    }
                }
            }
        }
        return List.copyOf(ids);
    }

    private static BigInteger historyId(String value) {
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
