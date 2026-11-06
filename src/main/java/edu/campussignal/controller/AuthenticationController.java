package edu.campussignal.controller;

import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.security.AuthenticatedUserService;
import edu.campussignal.dto.SessionResponse;
import edu.campussignal.service.GoogleWebOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final GoogleWebOAuthService google; private final AuthenticatedUserService sessions; private final UserProfileRepository profiles; private final String frontendUri; private final boolean secureCookie;
    public AuthenticationController(GoogleWebOAuthService google, AuthenticatedUserService sessions, UserProfileRepository profiles, @org.springframework.beans.factory.annotation.Value("${campussignal.frontend-uri}") String frontendUri, @org.springframework.beans.factory.annotation.Value("${campussignal.cookie-secure:false}") boolean secureCookie) { this.google = google; this.sessions = sessions; this.profiles = profiles; this.frontendUri = frontendUri; this.secureCookie = secureCookie; }
    @GetMapping("/google/login") public void login(HttpServletResponse response) throws java.io.IOException { response.sendRedirect(google.authorizationUrl()); }
    @GetMapping("/google/callback") public void callback(String code, String state, HttpServletResponse response) throws Exception {
        var account = google.complete(code, state); var grant = sessions.create(account);
        response.addHeader("Set-Cookie", ResponseCookie.from(AuthenticatedUserService.SESSION_COOKIE, grant.sessionId()).httpOnly(true).secure(secureCookie).sameSite("Lax").path("/").maxAge(Duration.ofHours(8)).build().toString());
        response.sendRedirect(frontendUri);
    }
    @GetMapping("/session") public SessionResponse session(HttpServletRequest request) {
        var user = sessions.current(request); return new SessionResponse(user.getGoogleEmail(), user.getDisplayName(), profiles.findByUserId(user.getId()).isPresent(), google.connected(user), sessions.csrf(request));
    }
    @PostMapping("/logout") public void logout(@RequestHeader(value = "X-CampusSignal-CSRF", required = false) String csrf, HttpServletRequest request, HttpServletResponse response) {
        sessions.currentForMutation(request, csrf); sessions.logout(request); response.addHeader("Set-Cookie", ResponseCookie.from(AuthenticatedUserService.SESSION_COOKIE, "").httpOnly(true).secure(secureCookie).sameSite("Lax").path("/").maxAge(Duration.ZERO).build().toString());
    }
    @PostMapping("/gmail/disconnect") public void disconnect(@RequestHeader(value = "X-CampusSignal-CSRF", required = false) String csrf, HttpServletRequest request) { google.disconnect(sessions.currentForMutation(request, csrf)); }
}
