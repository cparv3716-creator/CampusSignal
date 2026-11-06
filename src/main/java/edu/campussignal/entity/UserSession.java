package edu.campussignal.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_sessions", uniqueConstraints = @UniqueConstraint(
        name = "uk_session_token_hash", columnNames = "token_hash"))
public class UserSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "csrf_token", nullable = false, length = 128) private String csrfToken;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    protected UserSession() { }
    public UserSession(UserAccount user, String tokenHash, String csrfToken, Instant expiresAt) {
        this.user = user; this.tokenHash = tokenHash; this.csrfToken = csrfToken; this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getCsrfToken() { return csrfToken; }
    public Instant getExpiresAt() { return expiresAt; }
}
