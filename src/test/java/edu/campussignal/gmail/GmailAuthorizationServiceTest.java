package edu.campussignal.gmail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import edu.campussignal.config.GmailProperties;
import static org.assertj.core.api.Assertions.*;

class GmailAuthorizationServiceTest {
    @TempDir Path directory;

    private GmailAuthorizationService service() throws IOException {
        Path clientFile = directory.resolve("synthetic-client.json");
        // Deliberately synthetic test values; never load local account credentials.
        Files.writeString(clientFile, """
                {"installed":{"client_id":"synthetic-test-client","client_secret":"unused-test-value",
                "auth_uri":"https://accounts.google.com/o/oauth2/auth",
                "token_uri":"https://oauth2.googleapis.com/token","redirect_uris":["http://localhost"]}}
                """);
        return new GmailAuthorizationService(new GmailProperties(clientFile, directory.resolve("tokens"), 8888, 10));
    }

    @Test
    void usesReadOnlyScopeAndOfflineAccess() throws Exception {
        var flow = service().createFlow(new QueuedHttpTransport());
        assertThat(flow.getScopes()).containsExactly(GmailScopes.GMAIL_READONLY);
        assertThat(flow.newAuthorizationUrl().getAccessType()).isEqualTo("offline");
    }

    @Test
    void retrievalWithoutStoredAuthorizationFailsWithoutOpeningBrowser() throws Exception {
        var authorization = service();
        var flow = authorization.createFlow(new QueuedHttpTransport());
        assertThatThrownBy(() -> authorization.authorize(flow, false)).isInstanceOf(IOException.class)
                .hasMessageContaining("authorize command");
    }

    @Test
    void savedCredentialIsReusedAndExpiredTokenRefreshIsAutomaticAndPersisted() throws Exception {
        var transport = new QueuedHttpTransport()
                .enqueue("{\"access_token\":\"synthetic-refreshed-token\",\"expires_in\":3600,\"token_type\":\"Bearer\"}")
                .enqueue("{}");
        var authorization = service();
        var flow = authorization.createFlow(transport);
        flow.createAndStoreCredential(new TokenResponse().setAccessToken("synthetic-expired-token")
                .setRefreshToken("synthetic-refresh-token").setExpiresInSeconds(-60L), "local-user");
        var credential = authorization.authorize(flow, false);
        var gmail = new Gmail.Builder(transport, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("CampusSignal tests").build();
        assertThat(new GmailApiClient(gmail).listMessageIds(10)).isEmpty();
        assertThat(flow.loadCredential("local-user").getAccessToken()).isEqualTo("synthetic-refreshed-token");
        assertThat(transport.urls).anyMatch(url -> url.equals("https://oauth2.googleapis.com/token"));
        assertThat(transport.requests).anyMatch(request ->
                "Bearer synthetic-refreshed-token".equals(request.getFirstHeaderValue("Authorization")));
    }

    @Test
    void missingClientFileHasAnActionableSafeError() {
        var authorization = new GmailAuthorizationService(
                new GmailProperties(directory.resolve("missing.json"), directory.resolve("tokens"), 8888, 10));
        assertThatThrownBy(() -> authorization.createFlow(new QueuedHttpTransport()))
                .isInstanceOf(IOException.class).hasMessageContaining("GMAIL_CREDENTIALS_FILE");
    }
}
