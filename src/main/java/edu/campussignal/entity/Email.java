package edu.campussignal.entity;

import java.time.Instant;
import jakarta.persistence.*;
import edu.campussignal.dto.IncomingEmail;

@Entity
@Table(name = "emails", uniqueConstraints = @UniqueConstraint(
        name = "uk_email_gmail_message_id", columnNames = "gmail_message_id"))
public class Email {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gmail_message_id", nullable = false, length = 255)
    private String gmailMessageId;

    @Lob @Column(nullable = false)
    private String subject;

    @Lob @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private Instant receivedAt;

    @Lob @Column(nullable = false)
    private String body;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Email() {}

    public Email(IncomingEmail incoming) {
        gmailMessageId = incoming.messageId();
        subject = incoming.subject();
        sender = incoming.sender();
        receivedAt = incoming.receivedAt();
        body = incoming.body();
    }

    @PrePersist
    void initializeTimestamp() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getGmailMessageId() { return gmailMessageId; }
    public String getSubject() { return subject; }
    public String getSender() { return sender; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
