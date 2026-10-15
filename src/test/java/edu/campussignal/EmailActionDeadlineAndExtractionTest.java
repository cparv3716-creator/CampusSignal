package edu.campussignal;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.entity.Email;
import edu.campussignal.gmail.GmailAuthorizationService;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.EmailClassificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailActionDeadlineAndExtractionTest {
    private static final Instant CURRENT_TIME = Instant.parse("2026-10-18T12:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired EmailActionRepository actions;
    @Autowired EmailRepository emails;
    @MockitoBean Clock actionClock;
    @MockitoSpyBean EmailClassificationService classifier;
    @MockitoSpyBean GmailAuthorizationService gmailAuthorization;

    @BeforeEach
    void clearState() {
        actions.deleteAll();
        emails.deleteAll();
        clearInvocations(actionClock, classifier, gmailAuthorization);
        when(actionClock.instant()).thenReturn(CURRENT_TIME);
    }

    @Test
    void pendingActionWithFutureDeadlineRemainsEffectivelyPending() throws Exception {
        long actionId = createAction(email("future", "Future deadline", "Apply soon").getId(),
                "2026-10-19");

        assertEffectiveStatus(actionId, "PENDING", "PENDING");
    }

    @Test
    void pendingActionWithTodaysDeadlineRemainsEffectivelyPending() throws Exception {
        long actionId = createAction(email("today", "Today's deadline", "Apply today").getId(),
                "2026-10-18");

        assertEffectiveStatus(actionId, "PENDING", "PENDING");
    }

    @Test
    void pendingActionWithPastDeadlineIsEffectivelyMissedWithoutPersistenceMutation()
            throws Exception {
        long actionId = createAction(email("past", "Past deadline", "Apply yesterday").getId(),
                "2026-10-17");

        assertEffectiveStatus(actionId, "PENDING", "MISSED");

        assertThat(actions.findById(actionId).orElseThrow().getStatus().name()).isEqualTo("PENDING");
    }

    @Test
    void pendingActionWithoutDeadlineRemainsEffectivelyPending() throws Exception {
        long actionId = createAction(email("no-deadline", "No deadline", "Apply").getId(), null);

        assertEffectiveStatus(actionId, "PENDING", "PENDING");
    }

    @Test
    void completedActionWithPastDeadlineRemainsEffectivelyCompleted() throws Exception {
        long actionId = createAction(email("completed", "Completed", "Apply").getId(),
                "2026-10-17");
        updateStatus(actionId, "COMPLETED");

        assertEffectiveStatus(actionId, "COMPLETED", "COMPLETED");
    }

    @Test
    void notInterestedActionWithPastDeadlineRemainsEffectivelyNotInterested() throws Exception {
        long actionId = createAction(email("dismissed", "Dismissed", "Attend").getId(),
                "2026-10-17");
        updateStatus(actionId, "NOT_INTERESTED");

        assertEffectiveStatus(actionId, "NOT_INTERESTED", "NOT_INTERESTED");
    }

    @Test
    void extractsApplyCandidateWithLongDateAndActionUrlWithoutMutation() throws Exception {
        var stored = email(
                "apply-candidate",
                "Research internship applications",
                "Applications for the research internship close on 18 October 2026. "
                        + "Apply here: https://example.org/apply");
        String originalBody = stored.getBody();
        clearInvocations(classifier);

        mvc.perform(get("/api/v1/emails/{emailId}/action-candidate", stored.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("APPLY"))
                .andExpect(jsonPath("$.deadline").value("2026-10-18"))
                .andExpect(jsonPath("$.actionUrl").value("https://example.org/apply"));

        var unchanged = emails.findById(stored.getId()).orElseThrow();
        assertThat(unchanged.getBody()).isEqualTo(originalBody);
        assertThat(unchanged.getCategory()).isEqualTo(EmailCategory.CAREER);
        assertThat(actions.count()).isZero();
        verifyNoInteractions(classifier);
        verifyNoInteractions(gmailAuthorization);
    }

    @Test
    void extractsRegisterCandidateWithMonthFirstDeadline() throws Exception {
        var stored = email(
                "register-candidate",
                "Workshop registration",
                "Register by October 18, 2026 at https://events.example.org/register");

        assertCandidate(stored, "REGISTER", "2026-10-18",
                "https://events.example.org/register");
    }

    @Test
    void submitPhraseProducesSubmitCandidateWithIsoDeadline() throws Exception {
        var stored = email(
                "submit-candidate",
                "Scholarship form",
                "Submit the completed form by 2026-10-18. Upload it at "
                        + "https://forms.example.org/submit");

        assertCandidate(stored, "SUBMIT", "2026-10-18",
                "https://forms.example.org/submit");
    }

    @Test
    void explicitAttendanceRequestProducesAttendCandidate() throws Exception {
        var stored = email(
                "attend-candidate",
                "Required orientation",
                "Please attend the orientation on campus. Join details will follow.");

        assertCandidate(stored, "ATTEND", null, null);
    }

    @Test
    void informationalEventWithoutCallToActionProducesNoCandidate() throws Exception {
        assertNoCandidate(email(
                "informational-event",
                "Seminar tomorrow",
                "The systems seminar is tomorrow at 5 PM in Room 12."));
    }

    @Test
    void generalNewsletterProducesNoCandidate() throws Exception {
        assertNoCandidate(email(
                "newsletter",
                "October campus newsletter",
                "News from the library, sports centre, and student union."));
    }

    @Test
    void choosesActionContextUrlInsteadOfFooterUrl() throws Exception {
        var stored = email(
                "multiple-urls",
                "Internship application",
                "Privacy policy: https://example.org/privacy. "
                        + "Apply for the internship here: https://careers.example.org/apply");

        assertCandidate(stored, "APPLY", null, "https://careers.example.org/apply");
    }

    @Test
    void ignoresUnsafeUrlScheme() throws Exception {
        var stored = email(
                "unsafe-url",
                "Application action",
                "Apply here: javascript:alert(1). Applications close on 18 October 2026.");

        assertCandidate(stored, "APPLY", "2026-10-18", null);
    }

    @Test
    void ambiguousNumericDateDoesNotInventDeadline() throws Exception {
        var stored = email(
                "ambiguous-date",
                "Application deadline",
                "Apply by 10/11/2026 at https://example.org/apply");

        assertCandidate(stored, "APPLY", null, "https://example.org/apply");
    }

    @ParameterizedTest
    @ValueSource(strings = {"18 Oct 2026", "18/10/2026"})
    void supportsDocumentedAbbreviatedAndUnambiguousNumericDates(String date) throws Exception {
        var stored = email(
                "supported-date-" + date.replace('/', '-').replace(' ', '-'),
                "Application deadline",
                "Apply by " + date + " at https://example.org/apply");

        assertCandidate(stored, "APPLY", "2026-10-18", "https://example.org/apply");
    }

    @Test
    void completedAnnouncementProducesNoCandidate() throws Exception {
        assertNoCandidate(email(
                "completed-announcement",
                "Application received",
                "Your application was submitted successfully. No further action is required."));
    }

    @Test
    void missingEmailCandidateReturnsSafe404() throws Exception {
        mvc.perform(get("/api/v1/emails/{emailId}/action-candidate", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Email not found"));

        verifyNoInteractions(classifier);
        verifyNoInteractions(gmailAuthorization);
    }

    private void assertEffectiveStatus(long actionId, String persisted, String effective)
            throws Exception {
        mvc.perform(get("/api/v1/actions/{actionId}", actionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(persisted))
                .andExpect(jsonPath("$.effectiveStatus").value(effective));
    }

    private void assertCandidate(Email email, String type, String deadline, String actionUrl)
            throws Exception {
        var result = mvc.perform(get("/api/v1/emails/{emailId}/action-candidate", email.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value(type));
        if (deadline == null) {
            result.andExpect(jsonPath("$.deadline").isEmpty());
        } else {
            result.andExpect(jsonPath("$.deadline").value(deadline));
        }
        if (actionUrl == null) {
            result.andExpect(jsonPath("$.actionUrl").isEmpty());
        } else {
            result.andExpect(jsonPath("$.actionUrl").value(actionUrl));
        }
        assertThat(actions.count()).isZero();
        verifyNoInteractions(classifier);
        verifyNoInteractions(gmailAuthorization);
    }

    private void assertNoCandidate(Email email) throws Exception {
        mvc.perform(get("/api/v1/emails/{emailId}/action-candidate", email.getId()))
                .andExpect(status().isNoContent());
        assertThat(actions.count()).isZero();
        verifyNoInteractions(classifier);
        verifyNoInteractions(gmailAuthorization);
    }

    private long createAction(long emailId, String deadline) throws Exception {
        String deadlineField = deadline == null ? "" : ",\"deadline\":\"" + deadline + "\"";
        var response = mvc.perform(post("/api/v1/emails/{emailId}/actions", emailId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"APPLY\"" + deadlineField + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        String location = response.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private void updateStatus(long actionId, String actionStatus) throws Exception {
        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + actionStatus + "\"}"))
                .andExpect(status().isOk());
    }

    private Email email(String messageId, String subject, String body) {
        return emails.saveAndFlush(Email.classified(
                new IncomingEmail(
                        messageId,
                        subject,
                        "notices@example.edu",
                        Instant.parse("2026-10-08T08:00:00Z"),
                        body),
                new EmailClassificationResult(
                        EmailCategory.CAREER,
                        ClassificationConfidence.HIGH)));
    }
}
