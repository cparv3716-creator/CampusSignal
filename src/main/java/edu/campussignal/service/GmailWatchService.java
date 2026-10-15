package edu.campussignal.service;

import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import edu.campussignal.gmail.GmailApiClient;

@Service
public class GmailWatchService {
    public record Registration(String historyId, Instant expiresAt, String lastProcessedHistoryId) {}

    private final GmailWatchStateService state;

    public GmailWatchService(GmailWatchStateService state) {
        this.state = state;
    }

    public Registration register(GmailApiClient client, String topicPath) throws IOException {
        var watch = client.watchInbox(topicPath);
        var stored = state.recordWatch(watch.historyId(), watch.expiresAt());
        return new Registration(watch.historyId(), watch.expiresAt(), stored.lastHistoryId());
    }
}
