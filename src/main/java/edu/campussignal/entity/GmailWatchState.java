package edu.campussignal.entity;

import java.time.Instant;
import jakarta.persistence.*;

/** Singleton progress record for the one locally monitored Gmail mailbox. */
@Entity
@Table(name = "gmail_watch_state")
public class GmailWatchState {
    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(nullable = false, length = 64)
    private String lastHistoryId;

    @Column(nullable = false, length = 64)
    private String watchHistoryId;

    @Column(nullable = false)
    private Instant watchExpiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected GmailWatchState() {}

    public GmailWatchState(String historyId, Instant expiresAt) {
        id = SINGLETON_ID;
        lastHistoryId = historyId;
        watchHistoryId = historyId;
        watchExpiresAt = expiresAt;
    }

    public void renew(String historyId, Instant expiresAt) {
        // Renewal must not advance progress past history that has not been processed yet.
        watchHistoryId = historyId;
        watchExpiresAt = expiresAt;
    }

    public void advance(String historyId) {
        lastHistoryId = historyId;
    }

    @PrePersist
    void initializeTimestamps() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Integer getId() { return id; }
    public String getLastHistoryId() { return lastHistoryId; }
    public String getWatchHistoryId() { return watchHistoryId; }
    public Instant getWatchExpiresAt() { return watchExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
