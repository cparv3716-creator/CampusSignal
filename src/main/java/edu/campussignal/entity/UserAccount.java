package edu.campussignal.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A verified Google identity; this is deliberately distinct from Gmail credentials. */
@Entity
@Table(name = "user_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_google_subject", columnNames = "google_subject"),
        @UniqueConstraint(name = "uk_user_google_email", columnNames = "google_email")})
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "google_subject", nullable = false, updatable = false, length = 255)
    private String googleSubject;
    @Column(name = "google_email", nullable = false, length = 320) private String googleEmail;
    @Column(name = "display_name", length = 255) private String displayName;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected UserAccount() { }
    public UserAccount(String googleSubject, String googleEmail, String displayName) {
        this.googleSubject = googleSubject;
        this.googleEmail = googleEmail;
        this.displayName = displayName;
    }
    public void updateIdentity(String email, String name) { googleEmail = email; displayName = name; }
    @PrePersist void created() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getGoogleSubject() { return googleSubject; }
    public String getGoogleEmail() { return googleEmail; }
    public String getDisplayName() { return displayName; }
}
