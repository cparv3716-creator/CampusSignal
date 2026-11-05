package edu.campussignal.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.campussignal.dto.GmailSessionResponse;
import edu.campussignal.dto.GmailSyncRequest;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.security.LocalOperatorSessionService;
import edu.campussignal.security.LocalRequestSecurity;
import edu.campussignal.service.GmailSyncService;

@RestController
@RequestMapping("/api/v1/gmail")
public class GmailSyncController {
    private static final String SESSION_COOKIE = "CAMPUSSIGNAL_OPERATOR_SESSION";
    private final LocalOperatorSessionService sessions;
    private final LocalRequestSecurity requestSecurity;
    private final GmailSyncService sync;

    public GmailSyncController(LocalOperatorSessionService sessions,
                               LocalRequestSecurity requestSecurity, GmailSyncService sync) {
        this.sessions = sessions;
        this.requestSecurity = requestSecurity;
        this.sync = sync;
    }

    @PostMapping("/operator/bootstrap")
    public ResponseEntity<?> bootstrap(
            @RequestHeader(value = "X-CampusSignal-Operator-Secret", required = false)
            String secret, HttpServletRequest request) {
        if (!requestSecurity.isLoopback(request) || !requestSecurity.hasAllowedOrigin(request)
                || !sessions.matchesSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(sessions.issueBootstrapCode());
    }

    @PostMapping("/operator/session")
    public ResponseEntity<?> exchange(@RequestBody GmailSyncRequest input,
                                      HttpServletRequest request, HttpServletResponse response) {
        if (!requestSecurity.isLoopback(request) || !requestSecurity.hasAllowedOrigin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var grant = sessions.exchange(input == null ? null : input.code());
        if (grant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        response.addHeader("Set-Cookie", ResponseCookie.from(SESSION_COOKIE, grant.sessionId())
                .httpOnly(true).sameSite("Strict").path("/api/v1/gmail")
                .maxAge(java.time.Duration.ofHours(2)).build().toString());
        return ResponseEntity.ok(new GmailSessionResponse(grant.csrfToken()));
    }

    @PostMapping("/operator/session/restore")
    public ResponseEntity<?> restore(HttpServletRequest request) {
        if (!requestSecurity.isLoopback(request) || !requestSecurity.hasAllowedOrigin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var grant = sessions.restore(cookieValue(request, SESSION_COOKIE));
        if (grant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new GmailSessionResponse(grant.csrfToken()));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync(
            @RequestHeader(value = "X-CampusSignal-CSRF", required = false) String csrf,
            HttpServletRequest request) {
        if (!requestSecurity.isLoopback(request) || !requestSecurity.hasAllowedOrigin(request)
                || !sessions.valid(cookieValue(request, SESSION_COOKIE), csrf)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(sync.sync());
        } catch (GmailSyncService.SyncAlreadyRunningException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A Gmail sync is already running. Please wait and try again.");
        } catch (IOException | GeneralSecurityException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Gmail sync is unavailable. Check local authorization and connectivity.");
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Gmail sync could not be completed safely.");
        }
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
