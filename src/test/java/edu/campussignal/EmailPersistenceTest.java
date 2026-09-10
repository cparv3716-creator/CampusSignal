package edu.campussignal;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailPersistenceTest {
    @Autowired EmailRepository repository;
    @Autowired EmailIngestionService ingestion;
    @Autowired EmailRetrievalService retrieval;

    @BeforeEach
    void clearEmails() { repository.deleteAll(); }

    private IncomingEmail email(String id) {
        return new IncomingEmail(id, "Campus update", "notices@example.edu",
                Instant.parse("2026-09-08T10:30:00Z"), "Registration is open.");
    }

    @Test
    void persistsAllEmailFieldsWithAutomaticTimestamp() {
        var before = Instant.now();
        var result = ingestion.ingest(email("mail-1"));
        var stored = repository.findById(result.id()).orElseThrow();
        assertThat(result.created()).isTrue();
        assertThat(stored.getGmailMessageId()).isEqualTo("mail-1");
        assertThat(stored.getSubject()).isEqualTo("Campus update");
        assertThat(stored.getSender()).isEqualTo("notices@example.edu");
        assertThat(stored.getReceivedAt()).isEqualTo(Instant.parse("2026-09-08T10:30:00Z"));
        assertThat(stored.getBody()).isEqualTo("Registration is open.");
        assertThat(stored.getCreatedAt()).isBetween(before.minusMillis(1), Instant.now().plusMillis(1));
    }

    @Test
    void databaseConstraintRejectsDuplicateMessageIds() {
        repository.saveAndFlush(new Email(email("duplicate")));
        assertThatThrownBy(() -> repository.saveAndFlush(new Email(email("duplicate"))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void repeatedIngestionReturnsExistingRecord() {
        var first = ingestion.ingest(email("repeat"));
        var second = ingestion.ingest(email("repeat"));
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.created()).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicatesLeaveOneRecordAndBothCallsSucceed() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<EmailIngestionService.Result> task = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Test start timed out"); }
                return ingestion.ingest(email("concurrent"));
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var a = first.get(15, TimeUnit.SECONDS);
            var b = second.get(15, TimeUnit.SECONDS);
            assertThat(a.id()).isEqualTo(b.id());
            assertThat(a.created()).isNotEqualTo(b.created());
        }
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void retrievalPersistsMessagesAndCountsDuplicatesAndFailures() throws Exception {
        EmailSource source = new EmailSource() {
            public List<String> listMessageIds(int maximum) { return List.of("a", "bad", "b"); }
            public IncomingEmail fetch(String id) throws IOException {
                if (id.equals("bad")) { throw new IOException("simulated provider failure"); }
                return email(id);
            }
        };
        assertThat(retrieval.retrieve(source, 10)).isEqualTo(new RetrievalSummary(3, 2, 0, 1));
        assertThat(retrieval.retrieve(source, 10)).isEqualTo(new RetrievalSummary(3, 0, 2, 1));
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void listFailureIsExplicitAndWritesNothing() {
        EmailSource source = new EmailSource() {
            public List<String> listMessageIds(int maximum) throws IOException { throw new IOException("unavailable"); }
            public IncomingEmail fetch(String id) { throw new AssertionError("must not fetch"); }
        };
        assertThatThrownBy(() -> retrieval.retrieve(source, 10)).isInstanceOf(IOException.class);
        assertThat(repository.count()).isZero();
        assertThatThrownBy(() -> retrieval.retrieve(source, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> retrieval.retrieve(source, 501)).isInstanceOf(IllegalArgumentException.class);
    }
}
