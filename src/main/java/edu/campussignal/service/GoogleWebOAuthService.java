package edu.campussignal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import edu.campussignal.entity.GoogleAuthorizationAttempt;
import edu.campussignal.entity.GoogleCredential;
import edu.campussignal.entity.UserAccount;
import edu.campussignal.repository.GoogleAuthorizationAttemptRepository;
import edu.campussignal.repository.GoogleCredentialRepository;
import edu.campussignal.repository.UserAccountRepository;
import edu.campussignal.security.TokenCipher;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Server-side authorization-code OAuth flow. The browser never receives a client secret or refresh token. */
@Service
public class GoogleWebOAuthService {
    private static final String GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    private final String clientId, clientSecret, redirectUri, authorizeUri, tokenUri;
    private final ObjectMapper json;
    private final TokenCipher cipher;
    private final GoogleAuthorizationAttemptRepository attempts;
    private final UserAccountRepository users;
    private final GoogleCredentialRepository credentials;
    private final SecureRandom random = new SecureRandom();
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    public GoogleWebOAuthService(
            @Value("${campussignal.google.client-id:}") String clientId,
            @Value("${campussignal.google.client-secret:}") String clientSecret,
            @Value("${campussignal.google.redirect-uri:}") String redirectUri,
            @Value("${campussignal.google.authorize-uri}") String authorizeUri,
            @Value("${campussignal.google.token-uri}") String tokenUri,
            ObjectMapper json, TokenCipher cipher, GoogleAuthorizationAttemptRepository attempts,
            UserAccountRepository users, GoogleCredentialRepository credentials) {
        this.clientId = clientId; this.clientSecret = clientSecret; this.redirectUri = redirectUri;
        this.authorizeUri = authorizeUri; this.tokenUri = tokenUri; this.json = json; this.cipher = cipher;
        this.attempts = attempts; this.users = users; this.credentials = credentials;
    }

    @Transactional
    public String authorizationUrl() {
        configured();
        String state = token(), verifier = token(), nonce = token();
        attempts.save(new GoogleAuthorizationAttempt(hash(state), cipher.encrypt(verifier), cipher.encrypt(nonce),
                Instant.now().plusSeconds(600)));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest(verifier));
        return authorizeUri + "?" + form("client_id", clientId) + "&" + form("redirect_uri", redirectUri)
                + "&response_type=code&scope=" + encode("openid email profile " + GMAIL_SCOPE)
                + "&access_type=offline&include_granted_scopes=true&code_challenge_method=S256&code_challenge="
                + encode(challenge) + "&state=" + encode(state) + "&nonce=" + encode(nonce);
    }

    @Transactional
    public UserAccount complete(String code, String state) throws IOException, InterruptedException {
        if (code == null || state == null) throw new OAuthException("Google sign-in was not completed");
        GoogleAuthorizationAttempt attempt = attempts.findByStateHash(hash(state))
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new OAuthException("Google sign-in has expired; please try again"));
        attempts.delete(attempt); // state is single-use, including on token-exchange failure
        JsonNode token = exchange(code, cipher.decrypt(attempt.getEncryptedVerifier()));
        GoogleIdToken identity = verifyIdentity(requiredText(token, "id_token"), cipher.decrypt(attempt.getEncryptedNonce()));
        GoogleIdToken.Payload payload = identity.getPayload();
        String subject = payload.getSubject();
        String email = payload.getEmail();
        if (subject == null || email == null || !Boolean.TRUE.equals(payload.getEmailVerified()))
            throw new OAuthException("Google did not return a verified email address");
        String name = payload.get("name") instanceof String value ? value : null;
        UserAccount account = users.findByGoogleSubject(subject).orElseGet(() -> new UserAccount(subject, email, name));
        account.updateIdentity(email, name); account = users.save(account);
        final UserAccount savedAccount = account;
        String access = requiredText(token, "access_token");
        String refresh = optionalText(token, "refresh_token");
        long expires = token.path("expires_in").asLong(3600);
        String scope = optionalText(token, "scope");
        if (scope == null || !scope.contains(GMAIL_SCOPE)) throw new OAuthException("Gmail read-only permission was not granted");
        Instant expiry = Instant.now().plusSeconds(Math.max(1, expires));
        String encryptedRefresh = refresh == null ? null : cipher.encrypt(refresh);
        String encryptedAccess = cipher.encrypt(access);
        credentials.findByUserId(savedAccount.getId()).ifPresentOrElse(
                existing -> existing.replace(encryptedRefresh, encryptedAccess, expiry, scope),
                () -> { if (encryptedRefresh == null) throw new OAuthException("Google did not return offline access; reconnect Gmail");
                    credentials.save(new GoogleCredential(savedAccount, encryptedRefresh, encryptedAccess, expiry, scope)); });
        return account;
    }

    @Transactional
    public void disconnect(UserAccount user) { credentials.findByUserId(user.getId()).ifPresent(GoogleCredential::revoke); }

    @Transactional(readOnly = true)
    public boolean connected(UserAccount user) { return credentials.findByUserId(user.getId()).filter(value -> !value.isRevoked()).isPresent(); }

    @Transactional
    public String accessToken(UserAccount user) throws IOException, InterruptedException {
        GoogleCredential credential = credentials.findByUserId(user.getId()).filter(value -> !value.isRevoked())
                .orElseThrow(() -> new OAuthException("Gmail is not connected"));
        if (credential.getAccessExpiresAt().isAfter(Instant.now().plusSeconds(60))) return cipher.decrypt(credential.getEncryptedAccessToken());
        if (credential.getEncryptedRefreshToken() == null) throw new OAuthException("Gmail access has expired; reconnect Gmail");
        String body = form("client_id", clientId) + "&" + form("client_secret", clientSecret) + "&grant_type=refresh_token&" + form("refresh_token", cipher.decrypt(credential.getEncryptedRefreshToken()));
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(tokenUri)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() > 299) { credential.revoke(); throw new OAuthException("Gmail access was revoked; reconnect Gmail"); }
        JsonNode refreshed = json.readTree(response.body()); String access = requiredText(refreshed, "access_token");
        credential.replace(null, cipher.encrypt(access), Instant.now().plusSeconds(refreshed.path("expires_in").asLong(3600)), credential.getScopes());
        return access;
    }

    private JsonNode exchange(String code, String verifier) throws IOException, InterruptedException {
        String body = form("code", code) + "&" + form("client_id", clientId) + "&" + form("client_secret", clientSecret)
                + "&" + form("redirect_uri", redirectUri) + "&grant_type=authorization_code&" + form("code_verifier", verifier);
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(tokenUri))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() > 299) throw new OAuthException("Google authorization was declined or could not be verified");
        return json.readTree(response.body());
    }
    private GoogleIdToken verifyIdentity(String raw, String nonce) throws IOException {
        try {
            var verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(clientId)).build();
            GoogleIdToken token = verifier.verify(raw);
            if (token == null || !MessageDigest.isEqual(nonce.getBytes(StandardCharsets.UTF_8),
                    String.valueOf(token.getPayload().get("nonce")).getBytes(StandardCharsets.UTF_8)))
                throw new OAuthException("Google identity verification failed");
            return token;
        } catch (java.security.GeneralSecurityException exception) { throw new IOException("Google identity verification unavailable", exception); }
    }
    private void configured() { if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) throw new OAuthException("Google sign-in is not configured"); }
    private String token() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static byte[] digest(String value) { try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)); } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String form(String key, String value) { return encode(key) + "=" + encode(value); }
    private static String hash(String value) { return java.util.HexFormat.of().formatHex(digest(value)); }
    private static String requiredText(JsonNode node, String name) { String value = optionalText(node, name); if (value == null) throw new OAuthException("Google returned an incomplete authorization response"); return value; }
    private static String optionalText(JsonNode node, String name) { return node.hasNonNull(name) && node.get(name).isTextual() ? node.get(name).asText() : null; }
    public static class OAuthException extends RuntimeException { public OAuthException(String message) { super(message); } }
}
