package edu.campussignal;

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
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.EmailClassificationService;
import edu.campussignal.service.EmailIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class EmailClassificationIngestionTest {
    @Autowired EmailRepository repository;
    @Autowired EmailIngestionService ingestion;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean EmailClassificationService classifier;

    @BeforeEach
    void clearEmails() {
        repository.deleteAll();
    }

    @Test
    void newNormalizedEmailIsClassifiedAndPersisted() {
        var incoming = email(
                "classified-academic",
                "Department Seminar: Reliable Systems",
                "Speaker: Dr Example. Abstract: dependable distributed systems. Date: Friday.");

        var result = ingestion.ingest(incoming);

        var stored = repository.findById(result.id()).orElseThrow();
        assertThat(result.created()).isTrue();
        assertThat(stored.getCategory()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(stored.getClassificationConfidence()).isEqualTo(ClassificationConfidence.HIGH);
        assertThat(persistedConfidence("classified-academic")).isEqualTo(0.9);
        verify(classifier).classify(incoming.subject(), incoming.body());
    }

    @Test
    void duplicateSkipsClassificationAndRetainsPersistedClassification() {
        var original = email(
                "classified-duplicate",
                "Summer Internship Opportunity",
                "The employer offers a stipend and lists application eligibility.");
        var first = ingestion.ingest(original);
        clearInvocations(classifier);

        var duplicate = email(
                "classified-duplicate",
                "Film Screening — Friday 7 PM",
                "Students are invited to attend in Hall A.");
        var second = ingestion.ingest(duplicate);

        assertThat(second.created()).isFalse();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repository.count()).isEqualTo(1);
        var stored = repository.findById(first.id()).orElseThrow();
        assertThat(stored.getSubject()).isEqualTo(original.subject());
        assertThat(stored.getCategory()).isEqualTo(EmailCategory.CAREER);
        assertThat(stored.getClassificationConfidence()).isEqualTo(ClassificationConfidence.HIGH);
        verifyNoInteractions(classifier);
    }

    @Test
    void generalOtherLowClassificationIsPersisted() {
        var incoming = email(
                "classified-general",
                "Campus digest",
                "Thank you for reading this week's collection of updates.");

        var result = ingestion.ingest(incoming);

        var stored = repository.findById(result.id()).orElseThrow();
        assertThat(stored.getCategory()).isEqualTo(EmailCategory.GENERAL_OTHER);
        assertThat(stored.getClassificationConfidence()).isEqualTo(ClassificationConfidence.LOW);
        assertThat(persistedConfidence("classified-general")).isEqualTo(0.5);
    }

    @Test
    void classificationFailurePropagatesWithoutPersistingEmail() {
        var incoming = email("classification-failure", "Update", "Current message body.");
        var failure = new IllegalStateException("synthetic classification failure");
        doThrow(failure).when(classifier).classify(incoming.subject(), incoming.body());

        assertThatThrownBy(() -> ingestion.ingest(incoming)).isSameAs(failure);
        assertThat(repository.count()).isZero();
        verify(classifier).classify(incoming.subject(), incoming.body());
    }

    private IncomingEmail email(String id, String subject, String body) {
        return new IncomingEmail(id, subject, "notices@example.edu",
                Instant.parse("2026-09-08T10:30:00Z"), body);
    }

    private double persistedConfidence(String messageId) {
        return jdbc.queryForObject(
                "select classification_confidence from emails where gmail_message_id = ?",
                Double.class,
                messageId);
    }
}
