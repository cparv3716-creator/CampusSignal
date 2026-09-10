package edu.campussignal.gmail;

import java.io.IOException;
import java.time.Instant;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GmailApiClientTest {
    private GmailApiClient client(QueuedHttpTransport transport) {
        return new GmailApiClient(new Gmail.Builder(transport, GsonFactory.getDefaultInstance(),
                request -> request.setNumberOfRetries(0)).setApplicationName("CampusSignal tests").build());
    }

    @Test
    void paginatesInboxRequestsAndHonorsMaximum() throws Exception {
        var transport = new QueuedHttpTransport()
                .enqueue("{\"messages\":[{\"id\":\"a\"}],\"nextPageToken\":\"page2\"}")
                .enqueue("{\"messages\":[{\"id\":\"b\"},{\"id\":\"c\"}],\"nextPageToken\":\"page3\"}");
        assertThat(client(transport).listMessageIds(2)).containsExactly("a", "b");
        assertThat(transport.urls).hasSize(2);
        assertThat(transport.urls.getFirst()).contains("/users/me/messages", "labelIds=INBOX", "maxResults=2");
        assertThat(transport.urls.getLast()).contains("pageToken=page2", "maxResults=1");
    }

    @Test
    void emptyInboxAndInvalidLimitsAreHandled() throws Exception {
        var transport = new QueuedHttpTransport().enqueue("{}");
        assertThat(client(transport).listMessageIds(10)).isEmpty();
        assertThatThrownBy(() -> client(transport).listMessageIds(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client(transport).listMessageIds(501)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fetchesFullMessageAndMapsItsBody() throws Exception {
        var transport = new QueuedHttpTransport().enqueue("""
                {"id":"abc","internalDate":"1788861600000","payload":{"mimeType":"text/plain",
                "headers":[{"name":"Subject","value":"Campus update"}],"body":{"data":"SGVsbG8"}}}
                """);
        var message = client(transport).fetch("abc");
        assertThat(message.messageId()).isEqualTo("abc");
        assertThat(message.body()).isEqualTo("Hello");
        assertThat(message.subject()).isEqualTo("Campus update");
        assertThat(transport.urls.getFirst()).contains("/users/me/messages/abc", "format=full");
    }

    @Test
    void registersInboxWatchAndReadsHistory() throws Exception {
        var transport = new QueuedHttpTransport()
                .enqueue("{\"historyId\":\"123\",\"expiration\":1789000000000}")
                .enqueue("{\"history\":[{\"id\":\"124\",\"messagesAdded\":[{\"message\":{\"id\":\"m1\"}}]}],\"historyId\":\"125\"}")
                .enqueue("{\"historyId\":\"130\"}");
        var client = client(transport);
        var watch = client.watchInbox("projects/p/topics/mail");
        assertThat(watch.historyId()).isEqualTo("123");
        assertThat(watch.expiresAt()).isEqualTo(Instant.ofEpochMilli(1789000000000L));
        var history = client.listHistoryPage("123", null);
        assertThat(history.getHistory()).hasSize(1);
        assertThat(history.getHistory().getFirst().getMessagesAdded().getFirst().getMessage().getId())
                .isEqualTo("m1");
        assertThat(client.currentHistoryId()).isEqualTo("130");
        assertThat(transport.urls.get(0)).contains("/users/me/watch");
        assertThat(transport.urls.get(1)).contains("/users/me/history", "startHistoryId=123",
                "historyTypes=messageAdded", "labelId=INBOX");
        assertThat(transport.urls.get(2)).contains("/users/me/profile");
    }

    @Test
    void staleHistoryIsDistinguishedFromOtherProviderErrors() {
        var stale = new QueuedHttpTransport().enqueue(404, "{\"error\":{\"message\":\"gone\"}}");
        assertThatThrownBy(() -> client(stale).listHistoryPage("123", null))
                .isInstanceOf(StaleGmailHistoryException.class);
        var other = new QueuedHttpTransport().enqueue(401, "{\"error\":{\"message\":\"Unauthorized\"}}");
        assertThatThrownBy(() -> client(other).listHistoryPage("123", null))
                .isInstanceOf(IOException.class).isNotInstanceOf(StaleGmailHistoryException.class);
    }

    @Test
    void providerErrorsPropagateAndRepeatedPageTokensAreRejected() {
        var error = new QueuedHttpTransport().enqueue(401, "{\"error\":{\"message\":\"Unauthorized\"}}");
        assertThatThrownBy(() -> client(error).listMessageIds(10)).isInstanceOf(IOException.class);
        var repeated = new QueuedHttpTransport()
                .enqueue("{\"nextPageToken\":\"same\"}").enqueue("{\"nextPageToken\":\"same\"}");
        assertThatThrownBy(() -> client(repeated).listMessageIds(10)).isInstanceOf(IOException.class);
    }

    @Test
    void refusesMismatchedMessageIdentity() {
        var transport = new QueuedHttpTransport().enqueue("{\"id\":\"wrong\"}");
        assertThatThrownBy(() -> client(transport).fetch("expected")).isInstanceOf(IOException.class);
    }
}
