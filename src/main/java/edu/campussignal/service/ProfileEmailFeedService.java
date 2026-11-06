package edu.campussignal.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.dto.ActionUrgency;
import edu.campussignal.dto.EmailActionStatus;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.dto.RankedEmailFeedItemResponse;
import edu.campussignal.entity.EmailAction;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.entity.UserAccount;

@Service
@Transactional(readOnly = true)
public class ProfileEmailFeedService {
    private static final int DUE_TODAY_WEIGHT = 30;
    private static final int DUE_SOON_WEIGHT = 20;
    private static final int UPCOMING_WEIGHT = 10;
    private static final int ACTION_INDICATED_WEIGHT = 5;

    private final ProfileService profiles;
    private final EmailRepository emails;
    private final EmailActionRepository actions;
    private final CategoryScoreService categoryScores;
    private final DeterministicEmailActionExtractor actionExtractor;
    private final ActionDeadlinePresentationService deadlinePresentation;

    public ProfileEmailFeedService(
            ProfileService profiles,
            EmailRepository emails,
            EmailActionRepository actions,
            CategoryScoreService categoryScores,
            DeterministicEmailActionExtractor actionExtractor,
            ActionDeadlinePresentationService deadlinePresentation) {
        this.profiles = profiles;
        this.emails = emails;
        this.actions = actions;
        this.categoryScores = categoryScores;
        this.actionExtractor = actionExtractor;
        this.deadlinePresentation = deadlinePresentation;
    }

    public List<RankedEmailFeedItemResponse> feed(long profileId) {
        ProfileResponse profile = profiles.get(profileId);
        return feed(profile, emails.findAllByOrderByReceivedAtDescIdDesc());
    }
    public List<RankedEmailFeedItemResponse> feedForUser(UserAccount user) {
        ProfileResponse profile = ProfileResponse.from(profilesEntity(user));
        return feed(profile, emails.findAllByUserIdOrderByReceivedAtDescIdDesc(user.getId()));
    }
    private edu.campussignal.entity.UserProfile profilesEntity(UserAccount user) {
        return profiles.repositoryForOwner(user);
    }
    private List<RankedEmailFeedItemResponse> feed(ProfileResponse profile, List<edu.campussignal.entity.Email> ownedEmails) {
        Map<Long, EmailAction> latestActions = latestActionsByEmail();
        List<RankedEmailFeedItemResponse> ranked = new ArrayList<>();
        for (var email : ownedEmails) {
            if (email.getCategory() == null || email.getClassificationConfidence() == null) {
                continue;
            }
            var candidate = actionExtractor.extract(email.getSubject(), email.getBody()).orElse(null);
            var presentation = deadlinePresentation.present(
                    EmailActionStatus.PENDING, candidate == null ? null : candidate.deadline());
            int relevanceScore = (int) Math.round(categoryScores
                    .score(profile, categoryKey(email.getCategory())).score()
                    * email.getClassificationConfidence().persistedValue() * 100);
            int rankingScore = relevanceScore
                    + urgencyWeight(presentation.urgency())
                    + (candidate == null ? 0 : ACTION_INDICATED_WEIGHT);
            EmailAction tracked = latestActions.get(email.getId());
            ranked.add(new RankedEmailFeedItemResponse(
                    email.getId(),
                    email.getSubject(),
                    email.getCategory(),
                    email.getClassificationConfidence(),
                    relevanceScore,
                    rankingScore,
                    explanation(email.getCategory(), relevanceScore, presentation.urgency(), candidate != null, tracked),
                    candidate == null ? null : candidate.deadline(),
                    candidate == null ? null : candidate.actionType(),
                    candidate == null ? null : candidate.actionUrl(),
                    presentation.urgency(),
                    presentation.daysRemaining(),
                    candidate != null,
                    tracked != null,
                    tracked == null ? null : tracked.getStatus(),
                    email.getReceivedAt()));
        }
        return ranked.stream()
                .sorted(Comparator.comparingInt(RankedEmailFeedItemResponse::rankingScore)
                        .reversed()
                        .thenComparing(
                                RankedEmailFeedItemResponse::receivedAt,
                                Comparator.reverseOrder())
                        .thenComparing(
                                RankedEmailFeedItemResponse::emailId,
                                Comparator.reverseOrder()))
                .toList();
    }

    private Map<Long, EmailAction> latestActionsByEmail() {
        Map<Long, EmailAction> latest = new HashMap<>();
        for (EmailAction action : actions.findAllByOrderByCreatedAtDescIdDesc()) {
            latest.putIfAbsent(action.getEmail().getId(), action);
        }
        return latest;
    }

    private static String explanation(
            EmailCategory category,
            int relevanceScore,
            ActionUrgency urgency,
            boolean actionIndicated,
            EmailAction tracked) {
        StringBuilder explanation = new StringBuilder(categoryDescription(category));
        if (relevanceScore >= 30) {
            explanation.append(" It matches your saved academic profile or interests.");
        } else {
            explanation.append(" It is shown from your inbox but is a lower profile match.");
        }
        explanation.append(' ').append(deadlineDescription(urgency));
        if (actionIndicated) explanation.append(" A potential action was detected in the message.");
        if (tracked != null) explanation.append(" You are already tracking an action for it.");
        return explanation.toString();
    }

    private static String categoryDescription(EmailCategory category) {
        return switch (category) {
            case ACADEMIC_RESEARCH -> "Academic or research announcement.";
            case CAREER -> "Career or placement announcement.";
            case EVENT_ACTIVITY -> "Campus event or activity announcement.";
            case CALL_FOR_PARTICIPATION -> "Call for participation announcement.";
            case ADMINISTRATIVE_NOTICE -> "Administrative campus notice.";
            case WELLBEING_SUPPORT -> "Student wellbeing or support notice.";
            case RECOGNITION_ACHIEVEMENT -> "Recognition or achievement announcement.";
            case GENERAL_OTHER -> "General campus announcement.";
        };
    }

    private static String deadlineDescription(ActionUrgency urgency) {
        return switch (urgency) {
            case DUE_TODAY -> "The supported deadline is today.";
            case DUE_SOON -> "A supported deadline is due soon.";
            case UPCOMING -> "A supported deadline is upcoming.";
            case MISSED -> "The supported deadline has passed.";
            case COMPLETED -> "The related action is complete.";
            case NOT_INTERESTED -> "You marked the related action as not interested.";
            case NO_DEADLINE -> "No deadline was identified.";
        };
    }

    private static String categoryKey(EmailCategory category) {
        return category.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String relevance(int score) {
        if (score >= 50) return "HIGH";
        if (score >= 30) return "MEDIUM";
        return "LOW";
    }

    private static int urgencyWeight(ActionUrgency urgency) {
        return switch (urgency) {
            case DUE_TODAY -> DUE_TODAY_WEIGHT;
            case DUE_SOON -> DUE_SOON_WEIGHT;
            case UPCOMING -> UPCOMING_WEIGHT;
            case NO_DEADLINE, COMPLETED, NOT_INTERESTED, MISSED -> 0;
        };
    }
}
