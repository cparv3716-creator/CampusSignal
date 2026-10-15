package edu.campussignal;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailActionStatus;
import edu.campussignal.dto.EmailActionType;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.EmailClassificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailActionApiTest {
    private static final Instant COMPLETED_AT = Instant.parse("2026-10-15T14:44:00Z");
    private static final Instant LATER = Instant.parse("2026-10-16T10:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired EmailActionRepository actions;
    @Autowired EmailRepository emails;
    @MockitoBean Clock actionClock;
    @MockitoSpyBean EmailClassificationService classifier;

    @BeforeEach
    void clearState() {
        actions.deleteAll();
        emails.deleteAll();
        clearInvocations(actionClock, classifier);
    }

    @Test
    void createsPendingActionForExistingEmail() throws Exception {
        var email = email("action-create", "Internship applications open");

        mvc.perform(post("/api/v1/emails/{emailId}/actions", email.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "APPLY",
                                  "deadline": "2026-10-18",
                                  "actionUrl": "https://careers.example.edu/apply"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/actions/\\d+")))
                .andExpect(jsonPath("$.emailId").value(email.getId()))
                .andExpect(jsonPath("$.emailSubject").value("Internship applications open"))
                .andExpect(jsonPath("$.actionType").value("APPLY"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.deadline").value("2026-10-18"))
                .andExpect(jsonPath("$.actionUrl").value("https://careers.example.edu/apply"))
                .andExpect(jsonPath("$.completedAt").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.emailBody").doesNotExist());

        var stored = actions.findAll().getFirst();
        assertThat(stored.getEmail().getId()).isEqualTo(email.getId());
        assertThat(stored.getActionType()).isEqualTo(EmailActionType.APPLY);
        assertThat(stored.getStatus()).isEqualTo(EmailActionStatus.PENDING);
        assertThat(stored.getDeadline()).hasToString("2026-10-18");
        assertThat(stored.getActionUrl()).isEqualTo("https://careers.example.edu/apply");
        assertThat(stored.getCompletedAt()).isNull();
        verifyNoInteractions(classifier);
    }

    @Test
    void missingEmailReturns404AndCreatesNoAction() throws Exception {
        mvc.perform(post("/api/v1/emails/{emailId}/actions", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REGISTER\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Email not found"));

        assertThat(actions.count()).isZero();
        verifyNoInteractions(classifier);
    }

    @Test
    void completingPendingActionUsesApplicationClock() throws Exception {
        long emailId = email("action-complete", "Registration required").getId();
        long actionId = createAction(emailId, "REGISTER");
        when(actionClock.instant()).thenReturn(COMPLETED_AT);

        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").value("2026-10-15T14:44:00Z"))
                .andExpect(jsonPath("$.emailId").value(emailId));

        var stored = actions.findById(actionId).orElseThrow();
        assertThat(stored.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(stored.getEmail().getId()).isEqualTo(emailId);
        verify(actionClock).instant();
        verifyNoInteractions(classifier);
    }

    @Test
    void repeatedCompletionRetainsOriginalTimestamp() throws Exception {
        long actionId = createAction(email("action-repeat", "Apply now").getId(), "APPLY");
        when(actionClock.instant()).thenReturn(COMPLETED_AT, LATER);

        updateStatus(actionId, "COMPLETED");
        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").value("2026-10-15T14:44:00Z"));

        assertThat(actions.findById(actionId).orElseThrow().getCompletedAt()).isEqualTo(COMPLETED_AT);
        verify(actionClock, times(1)).instant();
    }

    @Test
    void pendingActionCanBeMarkedNotInterestedWithoutCompletionTime() throws Exception {
        long actionId = createAction(email("action-dismiss", "Event invitation").getId(), "ATTEND");

        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_INTERESTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_INTERESTED"))
                .andExpect(jsonPath("$.completedAt").isEmpty());

        assertThat(actions.findById(actionId).orElseThrow().getCompletedAt()).isNull();
        verifyNoInteractions(actionClock, classifier);
    }

    @Test
    void notInterestedActionCannotBeCompleted() throws Exception {
        long actionId = createAction(email("action-invalid", "Optional event").getId(), "ATTEND");
        updateStatus(actionId, "NOT_INTERESTED");

        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Action status transition is not allowed"));

        var stored = actions.findById(actionId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(EmailActionStatus.NOT_INTERESTED);
        assertThat(stored.getCompletedAt()).isNull();
        verifyNoInteractions(actionClock, classifier);
    }

    @Test
    void completedActionCannotReturnToPending() throws Exception {
        long actionId = createAction(email("action-no-reopen", "Submit form").getId(), "SUBMIT");
        when(actionClock.instant()).thenReturn(COMPLETED_AT);
        updateStatus(actionId, "COMPLETED");

        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Action status transition is not allowed"));

        assertThat(actions.findById(actionId).orElseThrow().getStatus())
                .isEqualTo(EmailActionStatus.COMPLETED);
    }

    @Test
    void readsMultipleActionsInDeterministicCreationOrderWithoutReclassification() throws Exception {
        var email = email("action-read", "Hackathon registration and form");
        long first = createAction(email.getId(), "REGISTER");
        long second = createAction(email.getId(), "SUBMIT");
        clearInvocations(classifier);

        mvc.perform(get("/api/v1/actions/{actionId}", first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(email.getId()))
                .andExpect(jsonPath("$.emailSubject").value("Hackathon registration and form"))
                .andExpect(jsonPath("$.actionType").value("REGISTER"))
                .andExpect(jsonPath("$.emailBody").doesNotExist());
        mvc.perform(get("/api/v1/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));
        mvc.perform(get("/api/v1/emails/{emailId}/actions", email.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));

        assertThat(actions.count()).isEqualTo(2);
        var unchangedEmail = emails.findById(email.getId()).orElseThrow();
        assertThat(unchangedEmail.getCategory()).isEqualTo(EmailCategory.EVENT_ACTIVITY);
        assertThat(unchangedEmail.getClassificationConfidence()).isEqualTo(ClassificationConfidence.HIGH);
        verifyNoInteractions(classifier);
    }

    @Test
    void malformedActionUrlIsRejectedWithoutPersistenceOrNetworkUse() throws Exception {
        var email = email("action-url", "Register");

        mvc.perform(post("/api/v1/emails/{emailId}/actions", email.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actionType":"REGISTER","actionUrl":"not a web address"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(actions.count()).isZero();
        verifyNoInteractions(classifier);
    }

    @Test
    void missingActionReturnsSafe404() throws Exception {
        mvc.perform(get("/api/v1/actions/{actionId}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Action not found"));

        verifyNoInteractions(classifier);
    }

    private Email email(String messageId, String subject) {
        return emails.saveAndFlush(Email.classified(
                new IncomingEmail(
                        messageId,
                        subject,
                        "notices@example.edu",
                        Instant.parse("2026-10-08T08:00:00Z"),
                        "Private stored email body"),
                new EmailClassificationResult(
                        EmailCategory.EVENT_ACTIVITY,
                        ClassificationConfidence.HIGH)));
    }

    private long createAction(long emailId, String type) throws Exception {
        var response = mvc.perform(post("/api/v1/emails/{emailId}/actions", emailId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"" + type + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String location = response.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private void updateStatus(long actionId, String status) throws Exception {
        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }
}
