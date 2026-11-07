package edu.campussignal.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Encrypted OAuth material only. Never serialize this entity to an HTTP response. */
@Entity
@Table(name = "google_credentials", uniqueConstraints = @UniqueConstraint(
        name = "uk_google_credential_user", columnNames = "user_id"))
public class GoogleCredential {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(name = "encrypted_refresh_token", length = 4096) private String encryptedRefreshToken;
    @Column(name = "encrypted_access_token", nullable = false, length = 4096) private String encryptedAccessToken;
    @Column(name = "access_expires_at", nullable = false) private Instant accessExpiresAt;
    @Column(nullable = false, length = 1024) private String scopes;
    @Column(nullable = false) private boolean revoked;
    protected GoogleCredential() { }
    public GoogleCredential(UserAccount user, String refresh, String access, Instant expiresAt, String scopes) {
        this.user = user; encryptedRefreshToken = refresh; encryptedAccessToken = access;
        accessExpiresAt = expiresAt; this.scopes = scopes; revoked = false;
    }
    public void replace(String refresh, String access, Instant expiresAt, String newScopes) {
        if (refresh != null) encryptedRefreshToken = refresh;
        encryptedAccessToken = access; accessExpiresAt = expiresAt; scopes = newScopes; revoked = false;
    }
    public void revoke() { revoked = true; encryptedAccessToken = ""; encryptedRefreshToken = null; }
    public UserAccount getUser() { return user; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public Instant getAccessExpiresAt() { return accessExpiresAt; }
    public String getScopes() { return scopes; }
    public boolean isRevoked() { return revoked; }
}
