package edu.campussignal.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import edu.campussignal.dto.ActionCandidate;
import edu.campussignal.dto.ActionUrgency;
import edu.campussignal.dto.DemoWorkspaceResponse;
import edu.campussignal.dto.EmailActionReadResponse;
import edu.campussignal.dto.EmailActionStatus;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailContentResponse;
import edu.campussignal.dto.EmailResponse;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.dto.RankedEmailFeedItemResponse;
import edu.campussignal.dto.StudentProfileRequest;
import edu.campussignal.dto.StudentProfileResponse;
import edu.campussignal.entity.UserAccount;

/**
 * Isolated in-memory demonstration data. It never reads, writes, or mixes with Gmail or user records.
 * Fixture messages are deliberately evaluated through the production classifier, scorer, action extractor,
 * and deadline presentation service rather than carrying precomputed results.
 */
@Service
public class DemoWorkspaceService {
    private static final long INTERNSHIP_ID = 900_001L;
    private static final long RESEARCH_ID = 900_002L;
    private static final long EVENT_ID = 900_003L;
    private static final int ACTION_WEIGHT = 5;
    private static final Map<ActionUrgency, Integer> URGENCY_WEIGHTS = Map.of(
            ActionUrgency.DUE_TODAY, 30, ActionUrgency.DUE_SOON, 20, ActionUrgency.UPCOMING, 10,
            ActionUrgency.NO_DEADLINE, 0, ActionUrgency.MISSED, 0,
            ActionUrgency.COMPLETED, 0, ActionUrgency.NOT_INTERESTED, 0);

    private static final List<FixtureEmail> FIXTURES = List.of(
            new FixtureEmail(INTERNSHIP_ID, "[DEMO DATA] Summer Internship 2027 — Economics & Data Analytics",
                    "careers@northstar-analytics.demo.campussignal.test",
                    Instant.parse("2026-11-16T08:30:00Z"), """
                    DEMO DATA — fictional opportunity for presentation use only.

                    Dear MA Economics students,

                    Northstar Analytics invites eligible MA Economics students graduating in 2027 to apply for our Summer Internship 2027 in Economics & Data Analytics. The internship includes applied economic research, data analysis, and mentorship with our analytics team.

                    The application deadline is November 22, 2026, 5 PM IST. Please apply with your CV and a short statement at https://apply.demo.campussignal.test/northstar-internship.

                    This is a fictional CampusSignal demo message. Do not send an application.
                    """),
            new FixtureEmail(RESEARCH_ID, "[DEMO DATA] Research Assistant Opening — Development Economics Project",
                    "research-office@policy-lab.demo.campussignal.test",
                    Instant.parse("2026-11-15T09:15:00Z"), """
                    DEMO DATA — fictional opportunity for presentation use only.

                    The Policy Lab is inviting applications for a Research Assistant opening on a Development Economics Project. The selected student will contribute to research design, evidence review, survey preparation, and data analysis in an ongoing development-economics research workshop.

                    The application deadline is December 1, 2026, 11:59 PM IST. Please apply with a CV and a short research note at https://apply.demo.campussignal.test/policy-lab-ra.

                    This is a fictional CampusSignal demo message. Do not send an application.
                    """),
            new FixtureEmail(EVENT_ID, "[DEMO DATA] Campus Art & Photography Evening — December 3, 2026",
                    "culture-desk@campus-life.demo.campussignal.test",
                    Instant.parse("2026-11-14T11:00:00Z"), """
                    DEMO DATA — fictional invitation for presentation use only.

                    You are invited to the Campus Art & Photography Evening on December 3, 2026 at 6 PM in the Student Centre Gallery. Join fellow students for a relaxed cultural evening featuring photography, illustration, and conversation with student artists.

                    No application or registration is required. This is a general invitation, not an opportunity with an application deadline.

                    This is a fictional CampusSignal demo message.
                    """));

    private final EmailClassificationService classifier;
    private final CategoryScoreService categoryScores;
    private final DeterministicEmailActionExtractor actionExtractor;
    private final ActionDeadlinePresentationService deadlinePresentation;
    private final Clock clock;
    private final Map<Long, DemoState> states = new ConcurrentHashMap<>();

    public DemoWorkspaceService(EmailClassificationService classifier, CategoryScoreService categoryScores,
            DeterministicEmailActionExtractor actionExtractor, ActionDeadlinePresentationService deadlinePresentation,
            Clock clock) {
        this.classifier = classifier;
        this.categoryScores = categoryScores;
        this.actionExtractor = actionExtractor;
        this.deadlinePresentation = deadlinePresentation;
        this.clock = clock;
    }

    public DemoWorkspaceResponse workspace(UserAccount owner) {
        return response(state(owner));
    }

    public DemoWorkspaceResponse saveProfile(UserAccount owner, StudentProfileRequest request) {
        DemoState state = state(owner);
        state.profile = request;
        return response(state);
    }

    public DemoWorkspaceResponse reset(UserAccount owner) {
        DemoState state = new DemoState(defaultProfile(), new HashMap<>());
        states.put(ownerId(owner), state);
        return response(state);
    }

    public EmailContentResponse content(UserAccount owner, long emailId) {
        FixtureEmail email = fixture(emailId);
        state(owner);
        return new EmailContentResponse(email.id(), email.subject(), email.sender(), email.receivedAt(), email.body(), false);
    }

    public Optional<ActionCandidate> candidate(UserAccount owner, long emailId) {
        state(owner);
        FixtureEmail email = fixture(emailId);
        return actionExtractor.extract(email.subject(), email.body());
    }

    public DemoWorkspaceResponse createAction(UserAccount owner, long emailId) {
        DemoState state = state(owner);
        FixtureEmail email = fixture(emailId);
        ActionCandidate candidate = actionExtractor.extract(email.subject(), email.body())
                .orElseThrow(DemoActionUnavailableException::new);
        state.actions.put(emailId, new DemoAction(candidate, EmailActionStatus.PENDING, Instant.now(clock), null));
        return response(state);
    }

    public DemoWorkspaceResponse updateAction(UserAccount owner, long emailId, EmailActionStatus status) {
        DemoState state = state(owner);
        DemoAction existing = state.actions.get(emailId);
        if (existing == null) throw new DemoActionNotFoundException();
        Instant completedAt = status == EmailActionStatus.COMPLETED ? Instant.now(clock) : null;
        state.actions.put(emailId, new DemoAction(existing.candidate(), status, existing.createdAt(), completedAt));
        return response(state);
    }

    private DemoWorkspaceResponse response(DemoState state) {
        StudentProfileRequest request = state.profile;
        StudentProfileResponse profile = new StudentProfileResponse(request.name(), "demo.student@demo.campussignal.test",
                request.college(), request.department(), request.programme(), request.year(), request.graduationYear(),
                request.semester(), request.interests());
        ProfileResponse scoringProfile = new ProfileResponse(null, profile.email(), profile.department(),
                profile.programme(), profile.year(), profile.semester(), profile.interests(), null);
        List<RankedEmailFeedItemResponse> feed = FIXTURES.stream()
                .map(email -> ranked(scoringProfile, email, state.actions.get(email.id())))
                .sorted(Comparator.comparingInt(RankedEmailFeedItemResponse::rankingScore).reversed()
                        .thenComparing(RankedEmailFeedItemResponse::receivedAt, Comparator.reverseOrder()))
                .toList();
        List<EmailResponse> emails = FIXTURES.stream().map(email -> {
            var classification = classifier.classify(email.subject(), email.body());
            return new EmailResponse(email.id(), email.subject(), email.sender(), email.receivedAt(),
                    classification.category(), classification.confidence().persistedValue());
        }).toList();
        List<EmailActionReadResponse> actions = state.actions.entrySet().stream()
                .map(entry -> actionResponse(entry.getKey(), entry.getValue())).toList();
        return new DemoWorkspaceResponse(true, profile, emails, feed, actions);
    }

    private RankedEmailFeedItemResponse ranked(ProfileResponse profile, FixtureEmail email, DemoAction tracked) {
        var classification = classifier.classify(email.subject(), email.body());
        var candidate = actionExtractor.extract(email.subject(), email.body()).orElse(null);
        var presentation = deadlinePresentation.present(EmailActionStatus.PENDING,
                candidate == null ? null : candidate.deadline());
        var score = categoryScores.score(profile, key(classification.category()));
        int relevance = (int) Math.round(score.score() * classification.confidence().persistedValue() * 100);
        int ranking = relevance + URGENCY_WEIGHTS.get(presentation.urgency()) + (candidate == null ? 0 : ACTION_WEIGHT);
        return new RankedEmailFeedItemResponse(email.id(), email.subject(), classification.category(),
                classification.confidence(), relevance, ranking,
                explanation(classification.category(), score.components().explicitPreference(), candidate != null, presentation.urgency()),
                candidate == null ? null : candidate.deadline(), candidate == null ? null : candidate.actionType(),
                candidate == null ? null : candidate.actionUrl(), presentation.urgency(), presentation.daysRemaining(),
                candidate != null, tracked != null, tracked == null ? null : tracked.status(), email.receivedAt());
    }

    private EmailActionReadResponse actionResponse(long emailId, DemoAction action) {
        FixtureEmail email = fixture(emailId);
        var presentation = deadlinePresentation.present(action.status(), action.candidate().deadline());
        return new EmailActionReadResponse(emailId, emailId, email.subject(), action.candidate().actionType(),
                action.status(), presentation.effectiveStatus(), presentation.urgency(), presentation.daysRemaining(),
                action.candidate().deadline(), action.candidate().actionUrl(), action.completedAt(), action.createdAt());
    }

    private String explanation(EmailCategory category, double interestMatch, boolean actionable, ActionUrgency urgency) {
        String match = interestMatch == 1.0 ? "Matches your saved " + interest(category) + "."
                : description(category) + " It does not currently match a saved interest.";
        String deadline = urgency == ActionUrgency.NO_DEADLINE ? " No deadline was identified."
                : " A supported deadline is upcoming.";
        return match + deadline + (actionable ? " An available action was detected." : "");
    }

    private static String key(EmailCategory category) { return category.name().toLowerCase(Locale.ROOT).replace('_', ' '); }
    private static String interest(EmailCategory category) { return switch (category) {
        case CAREER -> "career interests, including internships or placements";
        case ACADEMIC_RESEARCH -> "academic or research interests";
        case EVENT_ACTIVITY, CALL_FOR_PARTICIPATION -> "campus activities interests";
        default -> "student interests";
    }; }
    private static String description(EmailCategory category) { return switch (category) {
        case CAREER -> "Career or placement announcement.";
        case ACADEMIC_RESEARCH -> "Academic or research announcement.";
        case EVENT_ACTIVITY -> "Campus event or activity announcement.";
        case CALL_FOR_PARTICIPATION -> "Call for participation announcement.";
        default -> "General campus announcement.";
    }; }
    private DemoState state(UserAccount owner) { return states.computeIfAbsent(ownerId(owner), ignored -> new DemoState(defaultProfile(), new HashMap<>())); }
    private long ownerId(UserAccount owner) { return java.util.Objects.requireNonNull(owner.getId(), "Authenticated owner ID is required"); }
    private FixtureEmail fixture(long id) { return FIXTURES.stream().filter(email -> email.id() == id).findFirst().orElseThrow(DemoEmailNotFoundException::new); }
    private static StudentProfileRequest defaultProfile() { return new StudentProfileRequest("Demo Student", "Fictional University", "Economics", "MA Economics", 2, 2027, 3, List.of("internships", "research")); }
    private record FixtureEmail(long id, String subject, String sender, Instant receivedAt, String body) {}
    private record DemoAction(ActionCandidate candidate, EmailActionStatus status, Instant createdAt, Instant completedAt) {}
    private static final class DemoState { private StudentProfileRequest profile; private final Map<Long, DemoAction> actions; private DemoState(StudentProfileRequest profile, Map<Long, DemoAction> actions) { this.profile = profile; this.actions = actions; } }
    public static final class DemoEmailNotFoundException extends RuntimeException {}
    public static final class DemoActionNotFoundException extends RuntimeException {}
    public static final class DemoActionUnavailableException extends RuntimeException {}
}
