package edu.campussignal.entity;

import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.*;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private EmailCategory category;

    @Convert(converter = ClassificationConfidenceConverter.class)
    @Column(name = "classification_confidence")
    private ClassificationConfidence classificationConfidence;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Email() {}

    private Email(IncomingEmail incoming, EmailClassificationResult classification) {
        gmailMessageId = incoming.messageId();
        subject = incoming.subject();
        sender = incoming.sender();
        receivedAt = incoming.receivedAt();
        body = incoming.body();
        category = classification.category();
        classificationConfidence = classification.confidence();
    }

    public static Email classified(IncomingEmail incoming, EmailClassificationResult classification) {
        Objects.requireNonNull(incoming);
        return new Email(incoming, requireCompleteClassification(classification));
    }

    public void applyClassification(EmailClassificationResult classification) {
        var requiredClassification = requireCompleteClassification(classification);
        category = requiredClassification.category();
        classificationConfidence = requiredClassification.confidence();
    }

    private static EmailClassificationResult requireCompleteClassification(
            EmailClassificationResult classification) {
        var requiredClassification = Objects.requireNonNull(classification);
        Objects.requireNonNull(requiredClassification.category());
        Objects.requireNonNull(requiredClassification.confidence());
        return requiredClassification;
    }

    @PrePersist
    void initializeTimestamp() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getGmailMessageId() { return gmailMessageId; }
    public String getSubject() { return subject; }
    public String getSender() { return sender; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getBody() { return body; }
    public EmailCategory getCategory() { return category; }
    public ClassificationConfidence getClassificationConfidence() { return classificationConfidence; }
    public Instant getCreatedAt() { return createdAt; }
}
