package edu.campussignal;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.gmail.GmailApiClient;
import edu.campussignal.gmail.StaleGmailHistoryException;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.repository.GmailWatchStateRepository;
import edu.campussignal.service.GmailHistorySyncService;
import edu.campussignal.service.GmailWatchService;
import edu.campussignal.service.GmailWatchStateService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class GmailRealtimeServiceTest {
    @Autowired GmailWatchService watch;
    @Autowired GmailWatchStateService state;
    @Autowired GmailHistorySyncService history;
    @Autowired GmailWatchStateRepository stateRepository;
    @Autowired EmailRepository emailRepository;

    @BeforeEach
    void clear() {
        emailRepository.deleteAll();
        stateRepository.deleteAll();
    }

    private IncomingEmail email(String id) {
        return new IncomingEmail(id, "Update", "sender@example.edu",
                Instant.parse("2026-09-10T08:00:00Z"), "Body");
    }

    private ListHistoryResponse page(String historyId, String... messageIds) {
        var record = new History().setId(new BigInteger(historyId)).setMessagesAdded(
                java.util.Arrays.stream(messageIds)
                        .map(id -> new HistoryMessageAdded().setMessage(new Message().setId(id)))
                        .toList());
        return new ListHistoryResponse().setHistory(List.of(record))
                .setHistoryId(new BigInteger(historyId));
    }

    @Test
    void watchInitializationAndRenewalPreserveUnprocessedProgress() throws Exception {
        var client = mock(GmailApiClient.class);
        var firstExpiry = Instant.parse("2026-09-17T08:00:00Z");
        when(client.watchInbox("projects/p/topics/mail"))
                .thenReturn(new GmailApiClient.WatchRegistration("100", firstExpiry));
        var first = watch.register(client, "projects/p/topics/mail");
        assertThat(first.lastProcessedHistoryId()).isEqualTo("100");

        var renewedExpiry = Instant.parse("2026-09-18T08:00:00Z");
        when(client.watchInbox("projects/p/topics/mail"))
                .thenReturn(new GmailApiClient.WatchRegistration("150", renewedExpiry));
        state.advance("120");
        var renewed = watch.register(client, "projects/p/topics/mail");
        assertThat(renewed.lastProcessedHistoryId()).isEqualTo("120");
        assertThat(state.get().orElseThrow().watchHistoryId()).isEqualTo("150");
    }

    @Test
    void historyIngestsAddedMessagesAndAdvancesOnlyAfterSuccess() throws Exception {
        state.recordWatch("100", Instant.parse("2026-09-17T08:00:00Z"));
        var client = mock(GmailApiClient.class);
        when(client.listHistoryPage("100", null)).thenReturn(page("110", "m1", "m2"));
        when(client.fetch("m1")).thenReturn(email("m1"));
        when(client.fetch("m2")).thenReturn(email("m2"));

        var summary = history.sync(client, "110", 10);
        assertThat(summary.messagesFound()).isEqualTo(2);
        assertThat(summary.created()).isEqualTo(2);
        assertThat(state.get().orElseThrow().lastHistoryId()).isEqualTo("110");
        assertThat(emailRepository.count()).isEqualTo(2);

        assertThat(history.sync(client, "110", 10).messagesFound()).isZero();
        verify(client, times(1)).listHistoryPage("100", null);
    }

    @Test
    void failedMessageDoesNotAdvanceHistorySoRedeliveryCanRetry() throws Exception {
        state.recordWatch("100", Instant.parse("2026-09-17T08:00:00Z"));
        var client = mock(GmailApiClient.class);
        when(client.listHistoryPage("100", null)).thenReturn(page("110", "m1"));
        when(client.fetch("m1")).thenThrow(new IOException("synthetic failure"));
        assertThatThrownBy(() -> history.sync(client, "110", 10)).isInstanceOf(IOException.class);
        assertThat(state.get().orElseThrow().lastHistoryId()).isEqualTo("100");
    }

    @Test
    void staleHistoryRunsBoundedRecoveryAndResetsProgress() throws Exception {
        state.recordWatch("100", Instant.parse("2026-09-17T08:00:00Z"));
        var client = mock(GmailApiClient.class);
        when(client.listHistoryPage("100", null)).thenThrow(new StaleGmailHistoryException());
        when(client.listMessageIds(10)).thenReturn(List.of("recover"));
        when(client.fetch("recover")).thenReturn(email("recover"));
        when(client.currentHistoryId()).thenReturn("200");

        var summary = history.sync(client, "150", 10);
        assertThat(summary.recoveredFromStaleHistory()).isTrue();
        assertThat(summary.created()).isEqualTo(1);
        assertThat(state.get().orElseThrow().lastHistoryId()).isEqualTo("200");
    }
}
