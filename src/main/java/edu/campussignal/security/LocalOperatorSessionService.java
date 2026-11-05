package edu.campussignal.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalOperatorSessionService {
    private static final Duration BOOTSTRAP_LIFETIME = Duration.ofMinutes(1);
    private static final Duration SESSION_LIFETIME = Duration.ofHours(2);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> bootstrapCodes = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final String operatorSecret;

    public LocalOperatorSessionService(
            @Value("${campussignal.operator-secret:}") String operatorSecret) {
        this.operatorSecret = operatorSecret == null ? "" : operatorSecret;
    }

    public boolean isConfigured() {
        return operatorSecret.length() >= 32;
    }

    public boolean matchesSecret(String suppliedSecret) {
        if (!isConfigured() || suppliedSecret == null) {
            return false;
        }
        return MessageDigest.isEqual(
                operatorSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                suppliedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String issueBootstrapCode() {
        cleanup();
        String code = randomToken();
        bootstrapCodes.put(code, Instant.now().plus(BOOTSTRAP_LIFETIME));
        return code;
    }

    public SessionGrant exchange(String code) {
        cleanup();
        if (code == null || code.isBlank()) {
            return null;
        }
        Instant expiresAt = bootstrapCodes.remove(code);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            return null;
        }
        String sessionId = randomToken();
        String csrfToken = randomToken();
        Session session = new Session(csrfToken, Instant.now().plus(SESSION_LIFETIME));
        sessions.put(sessionId, session);
        return new SessionGrant(sessionId, csrfToken, session.expiresAt());
    }

    public boolean valid(String sessionId, String csrfToken) {
        cleanup();
        if (sessionId == null || sessionId.isBlank() || csrfToken == null) {
            return false;
        }
        Session session = sessions.get(sessionId);
        return session != null && MessageDigest.isEqual(
                session.csrfToken().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                csrfToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public SessionGrant restore(String sessionId) {
        cleanup();
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        Session current = sessions.get(sessionId);
        if (current == null) {
            return null;
        }
        String csrfToken = randomToken();
        Session refreshed = new Session(csrfToken, current.expiresAt());
        sessions.put(sessionId, refreshed);
        return new SessionGrant(sessionId, csrfToken, refreshed.expiresAt());
    }

    private void cleanup() {
        Instant now = Instant.now();
        bootstrapCodes.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record SessionGrant(String sessionId, String csrfToken, Instant expiresAt) {}
    private record Session(String csrfToken, Instant expiresAt) {}
}
