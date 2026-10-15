package edu.campussignal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.entity.Email;
import edu.campussignal.gmail.GmailAuthorizationService;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.DeterministicEmailActionExtractor;
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
class EmailActionUrgencyApiTest {
    private static final Instant TODAY = Instant.parse("2026-10-18T12:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired EmailActionRepository actions;
    @Autowired EmailRepository emails;
    @MockitoBean Clock actionClock;
    @MockitoSpyBean EmailClassificationService classifier;
    @MockitoSpyBean GmailAuthorizationService gmailAuthorization;
    @MockitoSpyBean DeterministicEmailActionExtractor actionExtractor;

    @BeforeEach
    void clearState() {
        actions.deleteAll();
        emails.deleteAll();
        clearInvocations(actionClock, classifier, gmailAuthorization, actionExtractor);
        when(actionClock.instant()).thenReturn(TODAY);
    }

    @Test
    void completedActionHasCompletedUrgencyWithoutCountdown() throws Exception {
        long actionId = createAction("completed-urgency", "2026-10-17");
        updateStatus(actionId, "COMPLETED");

        assertUrgency(actionId, "COMPLETED", null);
    }

    @Test
    void notInterestedActionHasNotInterestedUrgencyWithoutCountdown() throws Exception {
        long actionId = createAction("dismissed-urgency", "2026-10-17");
        updateStatus(actionId, "NOT_INTERESTED");

        assertUrgency(actionId, "NOT_INTERESTED", null);
    }

    @Test
    void expiredPendingActionHasMissedUrgencyWithoutNegativeCountdown() throws Exception {
        long actionId = createAction("missed-urgency", "2026-10-17");

        assertUrgency(actionId, "MISSED", null);
        assertThat(actions.findById(actionId).orElseThrow().getStatus().name()).isEqualTo("PENDING");
    }

    @Test
    void deadlineTodayIsDueTodayWithZeroDaysRemaining() throws Exception {
        long actionId = createAction("today-urgency", "2026-10-18");

        assertUrgency(actionId, "DUE_TODAY", 0);
        assertUrgency(actionId, "DUE_TODAY", 0);
    }

    @Test
    void deadlineTomorrowIsDueSoonWithOneDayRemaining() throws Exception {
        long actionId = createAction("tomorrow-urgency", "2026-10-19");

        assertUrgency(actionId, "DUE_SOON", 1);
    }

    @Test
    void deadlineInThreeDaysIsDueSoonAtUpperBoundary() throws Exception {
        long actionId = createAction("three-days-urgency", "2026-10-21");

        assertUrgency(actionId, "DUE_SOON", 3);
    }

    @Test
    void deadlineInFourDaysIsUpcomingAtLowerBoundary() throws Exception {
        long actionId = createAction("four-days-urgency", "2026-10-22");

        assertUrgency(actionId, "UPCOMING", 4);
    }

    @Test
    void farFutureDeadlineIsUpcomingWithExactDaysRemaining() throws Exception {
        long actionId = createAction("future-urgency", "2026-11-17");

        assertUrgency(actionId, "UPCOMING", 30);
    }

    @Test
    void actionWithoutDeadlineHasNoDeadlineUrgencyAndNoCountdown() throws Exception {
        long actionId = createAction("no-deadline-urgency", null);

        assertUrgency(actionId, "NO_DEADLINE", null);
    }

    @Test
    void advancingClockChangesDerivedUrgencyWithoutChangingStoredAction() throws Exception {
        long actionId = createAction("clock-advance", "2026-10-19");
        var before = actions.findById(actionId).orElseThrow();
        LocalDate storedDeadline = before.getDeadline();

        assertUrgency(actionId, "DUE_SOON", 1);
        when(actionClock.instant()).thenReturn(Instant.parse("2026-10-20T12:00:00Z"));
        assertUrgency(actionId, "MISSED", null);

        var unchanged = actions.findById(actionId).orElseThrow();
        assertThat(unchanged.getStatus().name()).isEqualTo("PENDING");
        assertThat(unchanged.getDeadline()).isEqualTo(storedDeadline);
    }

    @Test
    void urgencyDoesNotChangeExistingCollectionOrderingOrInvokeOtherSubsystems() throws Exception {
        var email = email("urgency-order");
        long first = createAction(email.getId(), "2026-10-18");
        long second = createAction(email.getId(), "2026-11-17");
        clearInvocations(classifier, gmailAuthorization, actionExtractor);

        mvc.perform(get("/api/v1/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[0].urgency").value("UPCOMING"))
                .andExpect(jsonPath("$[1].id").value(first))
                .andExpect(jsonPath("$[1].urgency").value("DUE_TODAY"));

        verifyNoInteractions(classifier, gmailAuthorization, actionExtractor);
    }

    private void assertUrgency(long actionId, String urgency, Integer daysRemaining)
            throws Exception {
        var result = mvc.perform(get("/api/v1/actions/{actionId}", actionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgency").value(urgency));
        if (daysRemaining == null) {
            result.andExpect(jsonPath("$.daysRemaining").isEmpty());
        } else {
            result.andExpect(jsonPath("$.daysRemaining").value(daysRemaining));
        }
        verifyNoInteractions(classifier, gmailAuthorization, actionExtractor);
    }

    private long createAction(String messageId, String deadline) throws Exception {
        return createAction(email(messageId).getId(), deadline);
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

    private void updateStatus(long actionId, String statusValue) throws Exception {
        mvc.perform(patch("/api/v1/actions/{actionId}/status", actionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + statusValue + "\"}"))
                .andExpect(status().isOk());
        clearInvocations(classifier, gmailAuthorization, actionExtractor);
    }

    private Email email(String messageId) {
        return emails.saveAndFlush(Email.classified(
                new IncomingEmail(
                        messageId,
                        "Action deadline",
                        "notices@example.edu",
                        Instant.parse("2026-10-08T08:00:00Z"),
                        "Stored private body"),
                new EmailClassificationResult(
                        EmailCategory.CAREER,
                        ClassificationConfidence.HIGH)));
    }
}
