package edu.campussignal.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.campussignal.demo.AuthenticatedDemoDataInitializer;
import edu.campussignal.dto.SessionResponse;
import edu.campussignal.repository.UserAccountRepository;
import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.security.AuthenticatedUserService;
import jakarta.servlet.http.HttpServletResponse;

/** Creates a real owner-scoped session, but only for the isolated fictional demo profile. */
@RestController
@Profile("demo")
@ConditionalOnProperty(name = "campussignal.demo-data.enabled", havingValue = "true")
@RequestMapping("/api/v1/auth/demo")
public class DemoAuthenticationController {
    private final UserAccountRepository users;
    private final UserProfileRepository profiles;
    private final AuthenticatedUserService sessions;
    private final boolean secureCookie;
    private final Duration sessionLifetime;

    public DemoAuthenticationController(
            UserAccountRepository users,
            UserProfileRepository profiles,
            AuthenticatedUserService sessions,
            @Value("${campussignal.cookie-secure:false}") boolean secureCookie,
            @Value("${campussignal.auth.session-hours:8}") long sessionHours) {
        this.users = users;
        this.profiles = profiles;
        this.sessions = sessions;
        this.secureCookie = secureCookie;
        this.sessionLifetime = Duration.ofHours(sessionHours);
    }

    @PostMapping
    public SessionResponse login(HttpServletResponse response) {
        var user = users.findByGoogleSubject(AuthenticatedDemoDataInitializer.DEMO_GOOGLE_SUBJECT)
                .orElseThrow(() -> new IllegalStateException("Fictional demo user was not initialized"));
        var grant = sessions.create(user);
        response.addHeader("Set-Cookie", ResponseCookie
                .from(AuthenticatedUserService.SESSION_COOKIE, grant.sessionId())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(sessionLifetime)
                .build()
                .toString());
        return new SessionResponse(
                user.getGoogleEmail(),
                user.getDisplayName(),
                profiles.findByUserId(user.getId()).isPresent(),
                false,
                grant.csrfToken());
    }
}
