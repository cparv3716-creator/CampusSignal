package edu.campussignal.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "google_authorization_attempts", uniqueConstraints = @UniqueConstraint(
        name = "uk_google_auth_state", columnNames = "state_hash"))
public class GoogleAuthorizationAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "state_hash", nullable = false, length = 64) private String stateHash;
    @Column(name = "encrypted_verifier", nullable = false, length = 1024) private String encryptedVerifier;
    @Column(name = "encrypted_nonce", nullable = false, length = 1024) private String encryptedNonce;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    protected GoogleAuthorizationAttempt() { }
    public GoogleAuthorizationAttempt(String stateHash, String verifier, String nonce, Instant expiresAt) {
        this.stateHash = stateHash; encryptedVerifier = verifier; encryptedNonce = nonce; this.expiresAt = expiresAt;
    }
    public String getStateHash() { return stateHash; }
    public String getEncryptedVerifier() { return encryptedVerifier; }
    public String getEncryptedNonce() { return encryptedNonce; }
    public Instant getExpiresAt() { return expiresAt; }
}
