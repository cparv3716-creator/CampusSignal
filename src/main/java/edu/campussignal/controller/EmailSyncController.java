package edu.campussignal.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.campussignal.config.GmailProperties;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.gmail.GmailAuthorizationService;
import edu.campussignal.service.EmailRetrievalService;

@RestController
@RequestMapping("/api/v1/emails")
@ConditionalOnProperty(
        name = "campussignal.gmail-sync.enabled",
        havingValue = "true")
@Profile("!prod")
public class EmailSyncController {

    private static final Set<String> LOOPBACK_ADDRESSES =
            Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    private final GmailAuthorizationService authorization;
    private final EmailRetrievalService retrieval;
    private final GmailProperties settings;
    private final byte[] syncToken;

    public EmailSyncController(
            GmailAuthorizationService authorization,
            EmailRetrievalService retrieval,
            GmailProperties settings,
            @Value("${campussignal.gmail-sync.token:}") String syncToken) {
        this.authorization = authorization;
        this.retrieval = retrieval;
        this.settings = settings;
        this.syncToken = syncToken.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/sync")
    public RetrievalSummary sync(
            HttpServletRequest request,
            @RequestHeader(
                    name = "X-CampusSignal-Sync-Token",
                    required = false) String suppliedToken) {

        if (!LOOPBACK_ADDRESSES.contains(request.getRemoteAddr())
                || !tokenMatches(suppliedToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            return retrieval.retrieve(
                    authorization.client(),
                    settings.maxResults());
        } catch (IOException | GeneralSecurityException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gmail synchronization unavailable");
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gmail synchronization failed safely");
        }
    }

    private boolean tokenMatches(String suppliedToken) {
        if (syncToken.length == 0 || suppliedToken == null || suppliedToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                syncToken,
                suppliedToken.getBytes(StandardCharsets.UTF_8));
    }
}
