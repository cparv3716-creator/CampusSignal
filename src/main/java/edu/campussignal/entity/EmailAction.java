package edu.campussignal.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionStatus;
import edu.campussignal.dto.EmailActionType;

@Entity
@Table(name = "email_actions")
public class EmailAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private EmailActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", nullable = false, length = 32)
    private EmailActionStatus status;

    private LocalDate deadline;

    @Column(name = "action_url", length = 2048)
    private String actionUrl;

    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailAction() {
        // Required by JPA.
    }

    private EmailAction(Email email, EmailActionCreateRequest request) {
        this.email = email;
        actionType = request.actionType();
        status = EmailActionStatus.PENDING;
        deadline = request.deadline();
        actionUrl = request.actionUrl();
    }

    public static EmailAction pending(Email email, EmailActionCreateRequest request) {
        return new EmailAction(Objects.requireNonNull(email), Objects.requireNonNull(request));
    }

    public void markCompleted(Instant completionTime) {
        status = EmailActionStatus.COMPLETED;
        completedAt = Objects.requireNonNull(completionTime);
    }

    public void markNotInterested() {
        status = EmailActionStatus.NOT_INTERESTED;
        completedAt = null;
    }

    @PrePersist
    void initializeTimestamp() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Email getEmail() { return email; }
    public EmailActionType getActionType() { return actionType; }
    public EmailActionStatus getStatus() { return status; }
    public LocalDate getDeadline() { return deadline; }
    public String getActionUrl() { return actionUrl; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
