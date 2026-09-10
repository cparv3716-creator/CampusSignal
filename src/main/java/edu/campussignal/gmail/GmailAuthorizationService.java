package edu.campussignal.gmail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.List;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.stereotype.Service;
import edu.campussignal.config.GmailProperties;

@Service
public class GmailAuthorizationService {
    private final GmailProperties settings;

    public GmailAuthorizationService(GmailProperties settings) { this.settings = settings; }

    GoogleAuthorizationCodeFlow createFlow(HttpTransport transport) throws IOException {
        if (!Files.isRegularFile(settings.credentialsFile())) {
            throw new IOException("Configure GMAIL_CREDENTIALS_FILE with a Desktop OAuth client file");
        }
        GoogleClientSecrets secrets;
        try (var reader = Files.newBufferedReader(settings.credentialsFile(), StandardCharsets.UTF_8)) {
            secrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), reader);
        }
        if (secrets.getInstalled() == null) {
            throw new IOException("A Desktop OAuth client is required");
        }
        return new GoogleAuthorizationCodeFlow.Builder(transport, GsonFactory.getDefaultInstance(),
                secrets, List.of(GmailScopes.GMAIL_READONLY))
                .setDataStoreFactory(new FileDataStoreFactory(settings.tokenDirectory().toFile()))
                .setAccessType("offline").build();
    }

    Credential authorize(GoogleAuthorizationCodeFlow flow, boolean interactive) throws IOException {
        Credential stored = flow.loadCredential("local-user");
        if (stored != null && (stored.getRefreshToken() != null
                || (stored.getAccessToken() != null && (stored.getExpiresInSeconds() == null
                || stored.getExpiresInSeconds() > 60)))) {
            // Credential refreshes expired access tokens automatically before API requests.
            return stored;
        }
        if (!interactive) {
            throw new IOException("Run the Gmail authorize command before retrieving emails");
        }
        var receiver = new LocalServerReceiver.Builder().setHost("127.0.0.1")
                .setPort(settings.callbackPort()).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("local-user");
    }

    public void authorize() throws IOException, GeneralSecurityException {
        authorize(createFlow(GoogleNetHttpTransport.newTrustedTransport()), true);
    }

    public GmailApiClient client() throws IOException, GeneralSecurityException {
        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var credential = authorize(createFlow(transport), false);
        var gmail = new Gmail.Builder(transport, GsonFactory.getDefaultInstance(), request -> {
            credential.initialize(request);
            request.setConnectTimeout(10_000);
            request.setReadTimeout(30_000);
        }).setApplicationName("CampusSignal").build();
        return new GmailApiClient(gmail, settings.userId());
    }
}
