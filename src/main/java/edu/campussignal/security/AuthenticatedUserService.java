package edu.campussignal.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.entity.UserAccount;
import edu.campussignal.entity.UserSession;
import edu.campussignal.repository.UserSessionRepository;

@Service
public class AuthenticatedUserService {
    public static final String SESSION_COOKIE = "CAMPUSSIGNAL_SESSION";
    private final UserSessionRepository sessions;
    private final SecureRandom random = new SecureRandom();
    private final boolean required;
    private final Duration lifetime;
    public AuthenticatedUserService(UserSessionRepository sessions,
            @Value("${campussignal.auth.required:true}") boolean required,
            @Value("${campussignal.auth.session-hours:8}") long sessionHours) {
        this.sessions = sessions; this.required = required; lifetime = Duration.ofHours(sessionHours);
    }
    public boolean required() { return required; }
    @Transactional
    public Grant create(UserAccount account) {
        sessions.deleteByExpiresAtBefore(Instant.now());
        String raw = randomToken(); String csrf = randomToken();
        sessions.save(new UserSession(account, hash(raw), csrf, Instant.now().plus(lifetime)));
        return new Grant(raw, csrf, Instant.now().plus(lifetime));
    }
    @Transactional(readOnly = true)
    public UserAccount current(HttpServletRequest request) {
        if (!required) return null;
        UserSession session = sessions.findByTokenHash(hash(cookie(request, SESSION_COOKIE)))
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(AuthenticationRequiredException::new);
        return session.getUser();
    }
    public UserAccount currentForMutation(HttpServletRequest request, String csrf) {
        UserAccount account = current(request);
        if (!required) return account;
        String token = sessions.findByTokenHash(hash(cookie(request, SESSION_COOKIE)))
                .orElseThrow(AuthenticationRequiredException::new).getCsrfToken();
        if (csrf == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), csrf.getBytes(StandardCharsets.UTF_8)))
            throw new CsrfRejectedException();
        return account;
    }
    @Transactional(readOnly = true)
    public String csrf(HttpServletRequest request) {
        if (!required) return "";
        return sessions.findByTokenHash(hash(cookie(request, SESSION_COOKIE)))
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(AuthenticationRequiredException::new).getCsrfToken();
    }
    @Transactional
    public void logout(HttpServletRequest request) {
        String value = cookie(request, SESSION_COOKIE);
        if (value != null) sessions.findByTokenHash(hash(value)).ifPresent(sessions::delete);
    }
    private String randomToken() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String value) {
        if (value == null) return "";
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static String cookie(HttpServletRequest request, String wanted) {
        Cookie[] cookies = request.getCookies(); if (cookies == null) return null;
        for (Cookie cookie : cookies) if (wanted.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
    public record Grant(String sessionId, String csrfToken, Instant expiresAt) { }
}
