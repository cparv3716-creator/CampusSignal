package edu.campussignal.gmail;

import java.io.IOException;
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
