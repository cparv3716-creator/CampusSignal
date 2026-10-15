package edu.campussignal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionType;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.dto.ProfileCreateRequest;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.entity.Email;
import edu.campussignal.entity.EmailAction;
import edu.campussignal.entity.UserProfile;
import edu.campussignal.gmail.GmailAuthorizationService;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.service.CategoryScoreService;
import edu.campussignal.service.DeterministicEmailActionExtractor;
import edu.campussignal.service.EmailClassificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileActionFeedApiTest {
    private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-18");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserProfileRepository profiles;
    @Autowired EmailRepository emails;
    @Autowired EmailActionRepository actions;
    @MockitoBean Clock actionClock;
    @MockitoSpyBean CategoryScoreService categoryScores;
    @MockitoSpyBean EmailClassificationService classifier;
    @MockitoSpyBean GmailAuthorizationService gmailAuthorization;
    @MockitoSpyBean DeterministicEmailActionExtractor actionExtractor;

    @BeforeEach
    void clearState() {
        actions.deleteAll();
        emails.deleteAll();
        profiles.deleteAll();
        clearInvocations(categoryScores, classifier, gmailAuthorization, actionExtractor, actionClock);
        when(actionClock.instant()).thenReturn(NOW);
    }

    @Test
    void strongCategoryMatchReceivesHigherExplainableRelevanceThroughExistingScorer()
            throws Exception {
        long profileId = profile("career");
        var strong = action(email("strong", EmailCategory.CAREER, ClassificationConfidence.HIGH), null);
        var weak = action(email("weak", EmailCategory.GENERAL_OTHER, ClassificationConfidence.HIGH), null);
        clearInvocations(categoryScores);

        JsonNode feed = feed(profileId);

        assertThat(ids(feed)).containsExactly(strong.getId(), weak.getId());
        assertThat(feed.get(0).get("relevance").asText()).isEqualTo("HIGH");
        assertThat(feed.get(0).get("relevanceScore").asInt()).isEqualTo(59);
        assertThat(feed.get(0).get("rankingExplanation").asText())
                .isEqualTo("HIGH relevance + NO_DEADLINE");
        assertThat(feed.get(1).get("relevance").asText()).isEqualTo("LOW");
        assertThat(feed.get(1).get("relevanceScore").asInt()).isEqualTo(23);
        verify(categoryScores, atLeastOnce()).score(any(ProfileResponse.class), eq("career"));
        verify(categoryScores, atLeastOnce()).score(any(ProfileResponse.class), eq("general other"));
    }

    @Test
    void deterministicClassificationConfidenceWeightsRelevanceButIsNotAProbability()
            throws Exception {
        long profileId = profile("career");
        var high = action(email("high", EmailCategory.CAREER, ClassificationConfidence.HIGH), null);
        var low = action(email("low", EmailCategory.CAREER, ClassificationConfidence.LOW), null);

        JsonNode feed = feed(profileId);

        assertThat(ids(feed)).containsExactly(high.getId(), low.getId());
        assertThat(feed.get(0).get("classificationConfidence").asText()).isEqualTo("HIGH");
        assertThat(feed.get(0).get("relevanceScore").asInt()).isEqualTo(59);
        assertThat(feed.get(1).get("classificationConfidence").asText()).isEqualTo("LOW");
        assertThat(feed.get(1).get("relevanceScore").asInt()).isEqualTo(33);
    }

    @Test
    void urgencyWeightsOrderOtherwiseEqualActionableItems() throws Exception {
        long profileId = profile("career");
        var noDeadline = action(email("none", EmailCategory.CAREER, ClassificationConfidence.HIGH), null);
        var upcoming = action(email("upcoming", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(4));
        var dueSoon = action(email("soon", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(2));
        var dueToday = action(email("today", EmailCategory.CAREER, ClassificationConfidence.HIGH), TODAY);

        JsonNode feed = feed(profileId);

        assertThat(ids(feed)).containsExactly(
                dueToday.getId(), dueSoon.getId(), upcoming.getId(), noDeadline.getId());
        assertThat(rankingScores(feed)).containsExactly(89, 79, 69, 59);
        assertThat(feed.get(0).get("daysRemaining").asInt()).isZero();
        assertThat(feed.get(1).get("daysRemaining").asInt()).isEqualTo(2);
        assertThat(feed.get(2).get("daysRemaining").asInt()).isEqualTo(4);
        assertThat(feed.get(3).get("daysRemaining").isNull()).isTrue();
    }

    @Test
    void documentedTradeoffLetsStrongMatchBeatWeakDueTodayItem() throws Exception {
        long profileId = profile("career");
        var strongNoDeadline = action(
                email("strong-tradeoff", EmailCategory.CAREER, ClassificationConfidence.HIGH), null);
        var weakDueToday = action(
                email("weak-tradeoff", EmailCategory.GENERAL_OTHER, ClassificationConfidence.HIGH), TODAY);

        JsonNode feed = feed(profileId);

        assertThat(ids(feed)).containsExactly(strongNoDeadline.getId(), weakDueToday.getId());
        assertThat(rankingScores(feed)).containsExactly(59, 53);
    }

    @Test
    void defaultFeedIncludesPendingButExcludesTerminalAndDerivedMissedItems() throws Exception {
        long profileId = profile("career");
        var pending = action(email("pending", EmailCategory.CAREER, ClassificationConfidence.HIGH), TODAY);
        var missed = action(email("missed", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.minusDays(1));
        var completed = action(email("completed", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(1));
        completed.markCompleted(NOW);
        actions.saveAndFlush(completed);
        var dismissed = action(email("dismissed", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(1));
        dismissed.markNotInterested();
        actions.saveAndFlush(dismissed);

        JsonNode feed = feed(profileId);

        assertThat(ids(feed)).containsExactly(pending.getId());
        assertThat(ids(feed)).doesNotContain(missed.getId(), completed.getId(), dismissed.getId());
        assertThat(feed.get(0).get("effectiveStatus").asText()).isEqualTo("PENDING");
    }

    @Test
    void deadlineThenRecencyAndIdProvideDeterministicTieBreakingAcrossRepeatedGets()
            throws Exception {
        long profileId = profile("career");
        var nearer = action(email("nearer", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(1));
        var farther = action(email("farther", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(3));
        var olderTie = action(email("older-tie", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(4));
        var newerTie = action(email("newer-tie", EmailCategory.CAREER, ClassificationConfidence.HIGH),
                TODAY.plusDays(4));

        List<Long> first = ids(feed(profileId));
        List<Long> second = ids(feed(profileId));

        assertThat(first).containsExactly(nearer.getId(), farther.getId(),
                newerTie.getId(), olderTie.getId());
        assertThat(second).isEqualTo(first);
    }

    @Test
    void sameActionsRankDifferentlyForProfilesWithDifferentInterests() throws Exception {
        long careerProfile = profile("career");
        long researchProfile = profile("academic research");
        var career = action(email("career-profile", EmailCategory.CAREER,
                ClassificationConfidence.HIGH), null);
        var research = action(email("research-profile", EmailCategory.ACADEMIC_RESEARCH,
                ClassificationConfidence.HIGH), null);

        assertThat(ids(feed(careerProfile))).containsExactly(career.getId(), research.getId());
        assertThat(ids(feed(researchProfile))).containsExactly(research.getId(), career.getId());
    }

    @Test
    void unknownProfileReturnsEstablishedSafe404() throws Exception {
        mvc.perform(get("/api/v1/profiles/{profileId}/action-feed", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Profile not found"));
    }

    @Test
    void repeatedFeedReadsDoNotMutateStateOrInvokeProtectedSubsystems() throws Exception {
        long profileId = profile("career");
        var action = action(email("isolated", EmailCategory.CAREER, ClassificationConfidence.MEDIUM),
                TODAY.plusDays(2));
        long profileCount = profiles.count();
        long emailCount = emails.count();
        long actionCount = actions.count();
        clearInvocations(classifier, gmailAuthorization, actionExtractor);

        feed(profileId);
        feed(profileId);

        var unchanged = actions.findById(action.getId()).orElseThrow();
        assertThat(profiles.count()).isEqualTo(profileCount);
        assertThat(emails.count()).isEqualTo(emailCount);
        assertThat(actions.count()).isEqualTo(actionCount);
        assertThat(unchanged.getStatus().name()).isEqualTo("PENDING");
        assertThat(unchanged.getDeadline()).isEqualTo(TODAY.plusDays(2));
        verifyNoInteractions(classifier, gmailAuthorization, actionExtractor);
    }

    private long profile(String interest) {
        var request = new ProfileCreateRequest(
                interest.replace(' ', '.') + "@example.edu",
                "Computer Science",
                "BTech",
                3,
                null,
                List.of(interest));
        return profiles.saveAndFlush(new UserProfile(request)).getId();
    }

    private Email email(
            String messageId,
            EmailCategory category,
            ClassificationConfidence confidence) {
        return emails.saveAndFlush(Email.classified(
                new IncomingEmail(
                        messageId,
                        "Action " + messageId,
                        "notices@example.edu",
                        Instant.parse("2026-10-08T08:00:00Z"),
                        "Stored private body"),
                new EmailClassificationResult(category, confidence)));
    }

    private EmailAction action(Email email, LocalDate deadline) {
        return actions.saveAndFlush(EmailAction.pending(
                email,
                new EmailActionCreateRequest(
                        EmailActionType.APPLY,
                        deadline,
                        "https://example.edu/action/" + email.getId())));
    }

    private JsonNode feed(long profileId) throws Exception {
        var response = mvc.perform(get("/api/v1/profiles/{profileId}/action-feed", profileId))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(response.getResponse().getContentAsString());
    }

    private List<Long> ids(JsonNode feed) {
        return feed.valueStream().map(item -> item.get("id").asLong()).toList();
    }

    private List<Integer> rankingScores(JsonNode feed) {
        return feed.valueStream().map(item -> item.get("rankingScore").asInt()).toList();
    }
}
