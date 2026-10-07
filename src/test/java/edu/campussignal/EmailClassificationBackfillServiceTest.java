package edu.campussignal;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.EmailClassificationBackfillService;
import edu.campussignal.service.EmailClassificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class EmailClassificationBackfillServiceTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-20T09:15:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-08-20T09:16:00Z");

    @Autowired EmailRepository repository;
    @Autowired EmailClassificationBackfillService backfill;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean EmailClassificationService classifier;

    @BeforeEach
    void clearEmailsAndClassifierInvocations() {
        repository.deleteAll();
        clearInvocations(classifier);
    }

    @Test
    void unclassifiedLegacyEmailIsClassifiedWithoutChangingMessageData() {
        insertStoredEmail(
                "legacy-academic",
                "Department Seminar: Reliable Systems",
                "Speaker: Dr Example. Abstract: dependable distributed systems.",
                null,
                null);

        var result = backfill.backfill();

        var stored = repository.findByGmailMessageId("legacy-academic").orElseThrow();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(stored.getCategory()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(stored.getClassificationConfidence()).isEqualTo(ClassificationConfidence.HIGH);
        assertThat(stored.getGmailMessageId()).isEqualTo("legacy-academic");
        assertThat(stored.getSubject()).isEqualTo("Department Seminar: Reliable Systems");
        assertThat(stored.getSender()).isEqualTo("legacy@example.edu");
        assertThat(stored.getReceivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(stored.getBody())
                .isEqualTo("Speaker: Dr Example. Abstract: dependable distributed systems.");
        assertThat(stored.getCreatedAt()).isEqualTo(CREATED_AT);
        verify(classifier).classify(stored.getSubject(), stored.getBody());
    }

    @Test
    void fullyClassifiedEmailIsSkippedWithoutInvokingClassifier() {
        var existing = repository.saveAndFlush(Email.classified(
                incoming("classified", "Campus digest", "Routine current-message update."),
                new EmailClassificationResult(EmailCategory.CAREER, ClassificationConfidence.MEDIUM)));
        var originalCreatedAt = storedCreatedAt("classified");
        clearInvocations(classifier);

        var result = backfill.backfill();

        var stored = repository.findById(existing.getId()).orElseThrow();
        assertThat(result.updated()).isZero();
        assertThat(stored.getCategory()).isEqualTo(EmailCategory.CAREER);
        assertThat(stored.getClassificationConfidence()).isEqualTo(ClassificationConfidence.MEDIUM);
        assertThat(stored.getSubject()).isEqualTo("Campus digest");
        assertThat(stored.getBody()).isEqualTo("Routine current-message update.");
        assertThat(stored.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(repository.count()).isEqualTo(1);
        verifyNoInteractions(classifier);
    }

    @Test
    void mixedDatasetUpdatesOnlyIncompleteRowsIncludingPartialLegacyState() {
        insertStoredEmail(
                "missing-both",
                "Summer Internship Opportunity",
                "The employer offers a stipend and lists application eligibility.",
                null,
                null);
        insertStoredEmail(
                "missing-confidence",
                "Film Screening - Friday 7 PM",
                "Students are invited to attend in Hall A.",
                EmailCategory.CAREER.name(),
                null);
        insertStoredEmail(
                "missing-category",
                "Counselling appointments",
                "The counsellor support service is available.",
                null,
                ClassificationConfidence.HIGH.persistedValue());
        var complete = repository.saveAndFlush(Email.classified(
                incoming("complete", "Existing classification", "Must remain untouched."),
                new EmailClassificationResult(
                        EmailCategory.RECOGNITION_ACHIEVEMENT,
                        ClassificationConfidence.LOW)));
        clearInvocations(classifier);

        var result = backfill.backfill();

        assertThat(result.updated()).isEqualTo(3);
        assertThat(repository.count()).isEqualTo(4);
        assertClassification("missing-both", EmailCategory.CAREER, ClassificationConfidence.HIGH);
        assertClassification("missing-confidence", EmailCategory.EVENT_ACTIVITY, ClassificationConfidence.HIGH);
        assertClassification(
                "missing-category",
                EmailCategory.WELLBEING_SUPPORT,
                ClassificationConfidence.HIGH);
        var unchanged = repository.findById(complete.getId()).orElseThrow();
        assertThat(unchanged.getCategory()).isEqualTo(EmailCategory.RECOGNITION_ACHIEVEMENT);
        assertThat(unchanged.getClassificationConfidence()).isEqualTo(ClassificationConfidence.LOW);
        verify(classifier).classify(
                "Summer Internship Opportunity",
                "The employer offers a stipend and lists application eligibility.");
        verify(classifier).classify(
                "Film Screening - Friday 7 PM",
                "Students are invited to attend in Hall A.");
        verify(classifier).classify(
                "Counselling appointments",
                "The counsellor support service is available.");
    }

    @Test
    void rerunIsIdempotentAndCreatesNoDuplicate() {
        insertStoredEmail(
                "legacy-rerun",
                "Campus digest",
                "Thank you for reading this week's collection of updates.",
                null,
                null);

        var first = backfill.backfill();
        var afterFirst = repository.findByGmailMessageId("legacy-rerun").orElseThrow();
        var firstId = afterFirst.getId();
        var firstCategory = afterFirst.getCategory();
        var firstConfidence = afterFirst.getClassificationConfidence();
        clearInvocations(classifier);

        var second = backfill.backfill();

        var afterSecond = repository.findByGmailMessageId("legacy-rerun").orElseThrow();
        assertThat(first.updated()).isEqualTo(1);
        assertThat(second.updated()).isZero();
        assertThat(afterSecond.getId()).isEqualTo(firstId);
        assertThat(afterSecond.getCategory()).isEqualTo(firstCategory);
        assertThat(afterSecond.getClassificationConfidence()).isEqualTo(firstConfidence);
        assertThat(repository.count()).isEqualTo(1);
        verifyNoInteractions(classifier);
    }

    @Test
    void classificationFailureStopsAndRollsBackTheWholeBackfill() {
        insertStoredEmail(
                "rollback-first",
                "Department Seminar",
                "Speaker and abstract are available.",
                null,
                null);
        insertStoredEmail(
                "rollback-failure",
                "Synthetic failure",
                "Synthetic failure body.",
                null,
                null);
        var failure = new IllegalStateException("synthetic classification failure");
        doThrow(failure).when(classifier).classify("Synthetic failure", "Synthetic failure body.");

        assertThatThrownBy(() -> backfill.backfill()).isSameAs(failure);

        assertClassification("rollback-first", null, null);
        assertClassification("rollback-failure", null, null);
        assertThat(repository.count()).isEqualTo(2);
    }

    private IncomingEmail incoming(String id, String subject, String body) {
        return new IncomingEmail(id, subject, "legacy@example.edu", RECEIVED_AT, body);
    }

    private void insertStoredEmail(
            String id,
            String subject,
            String body,
            String category,
            Double confidence) {
        jdbc.update("""
                insert into emails
                    (gmail_message_id, subject, sender, received_at, body,
                     category, classification_confidence, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                subject,
                "legacy@example.edu",
                Timestamp.from(RECEIVED_AT),
                body,
                category,
                confidence,
                Timestamp.from(CREATED_AT));
    }

    private void assertClassification(
            String messageId,
            EmailCategory category,
            ClassificationConfidence confidence) {
        var stored = repository.findByGmailMessageId(messageId).orElseThrow();
        assertThat(stored.getCategory()).isEqualTo(category);
        assertThat(stored.getClassificationConfidence()).isEqualTo(confidence);
    }

    private Instant storedCreatedAt(String messageId) {
        return jdbc.queryForObject(
                "select created_at from emails where gmail_message_id = ?",
                Timestamp.class,
                messageId).toInstant();
    }
}
