package edu.campussignal;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.EmailClassificationService;
import edu.campussignal.service.EmailIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailReadApiTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-09-08T10:30:00Z");

    @Autowired MockMvc mvc;
    @Autowired EmailRepository repository;
    @Autowired EmailIngestionService ingestion;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean EmailClassificationService classifier;

    @BeforeEach
    void clearEmailsAndClassifierInvocations() {
        repository.deleteAll();
        clearInvocations(classifier);
    }

    @Test
    void classifiedPersistedEmailIsExposedWithoutPrivateContent() throws Exception {
        var stored = repository.saveAndFlush(Email.classified(
                incoming("provider-private", "Career opportunity", "Private full body"),
                new EmailClassificationResult(EmailCategory.CAREER, ClassificationConfidence.MEDIUM)));

        mvc.perform(get("/api/v1/emails/{id}", stored.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stored.getId()))
                .andExpect(jsonPath("$.subject").value("Career opportunity"))
                .andExpect(jsonPath("$.sender").value("notices@example.edu"))
                .andExpect(jsonPath("$.receivedAt").value("2026-09-08T10:30:00Z"))
                .andExpect(jsonPath("$.category").value("CAREER"))
                .andExpect(jsonPath("$.classificationConfidence").value(0.7))
                .andExpect(jsonPath("$.body").doesNotExist())
                .andExpect(jsonPath("$.gmailMessageId").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());

        verifyNoInteractions(classifier);
    }

    @Test
    void ingestedEmailIsVisibleWithoutClassificationRunningAgain() throws Exception {
        var incoming = incoming(
                "ingested-read",
                "Department Seminar: Reliable Systems",
                "Speaker: Dr Example. Abstract: dependable distributed systems.");
        var ingested = ingestion.ingest(incoming);
        verify(classifier).classify(incoming.subject(), incoming.body());
        clearInvocations(classifier);

        mvc.perform(get("/api/v1/emails/{id}", ingested.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ACADEMIC_RESEARCH"))
                .andExpect(jsonPath("$.classificationConfidence").value(0.9));

        assertThat(repository.count()).isEqualTo(1);
        verifyNoInteractions(classifier);
    }

    @Test
    void collectionIsOrderedByReceivedTimeThenIdDescending() throws Exception {
        var older = persist("older", "Older", Instant.parse("2026-09-08T09:00:00Z"));
        var firstTie = persist("first-tie", "First tie", RECEIVED_AT);
        var secondTie = persist("second-tie", "Second tie", RECEIVED_AT);
        clearInvocations(classifier);

        mvc.perform(get("/api/v1/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(secondTie.getId()))
                .andExpect(jsonPath("$[1].id").value(firstTie.getId()))
                .andExpect(jsonPath("$[2].id").value(older.getId()));

        verifyNoInteractions(classifier);
    }

    @Test
    void missingEmailReturnsSafeProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/emails/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Email not found"));

        verifyNoInteractions(classifier);
    }

    @Test
    void incompleteLegacyClassificationReturnsSafeErrorWithoutMutation() throws Exception {
        jdbc.update("""
                insert into emails
                    (gmail_message_id, subject, sender, received_at, body,
                     category, classification_confidence, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "incomplete-read",
                "Legacy row",
                "legacy@example.edu",
                Timestamp.from(RECEIVED_AT),
                "Stored legacy body",
                null,
                ClassificationConfidence.HIGH.persistedValue(),
                Timestamp.from(RECEIVED_AT));
        long id = repository.findByGmailMessageId("incomplete-read").orElseThrow().getId();
        clearInvocations(classifier);

        mvc.perform(get("/api/v1/emails/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Stored email classification is incomplete"));

        assertThat(jdbc.queryForObject(
                "select category from emails where id = ?", String.class, id)).isNull();
        assertThat(jdbc.queryForObject(
                "select classification_confidence from emails where id = ?", Double.class, id))
                .isEqualTo(0.9);
        assertThat(repository.count()).isEqualTo(1);
        verifyNoInteractions(classifier);
    }

    private Email persist(String messageId, String subject, Instant receivedAt) {
        return repository.saveAndFlush(Email.classified(
                new IncomingEmail(messageId, subject, "notices@example.edu", receivedAt, "Private body"),
                new EmailClassificationResult(
                        EmailCategory.GENERAL_OTHER,
                        ClassificationConfidence.LOW)));
    }

    private IncomingEmail incoming(String messageId, String subject, String body) {
        return new IncomingEmail(messageId, subject, "notices@example.edu", RECEIVED_AT, body);
    }
}
