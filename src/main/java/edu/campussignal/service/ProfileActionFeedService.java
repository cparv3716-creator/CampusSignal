package edu.campussignal.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.dto.ActionRelevance;
import edu.campussignal.dto.ActionUrgency;
import edu.campussignal.dto.EmailActionEffectiveStatus;
import edu.campussignal.dto.EmailActionStatus;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.ProfileActionFeedItemResponse;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.entity.EmailAction;
import edu.campussignal.repository.EmailActionRepository;

@Service
@Transactional(readOnly = true)
public class ProfileActionFeedService {
    private static final int HIGH_RELEVANCE_MIN = 50;
    private static final int MEDIUM_RELEVANCE_MIN = 30;
    private static final int DUE_TODAY_WEIGHT = 30;
    private static final int DUE_SOON_WEIGHT = 20;
    private static final int UPCOMING_WEIGHT = 10;

    private static final Comparator<ProfileActionFeedItemResponse> RANKING_ORDER =
            Comparator.comparingInt(ProfileActionFeedItemResponse::rankingScore).reversed()
                    .thenComparing(Comparator.comparingInt(
                            ProfileActionFeedItemResponse::relevanceScore).reversed())
                    .thenComparing(Comparator.comparingInt(
                            (ProfileActionFeedItemResponse item) ->
                                    urgencyWeight(item.urgency())).reversed())
                    .thenComparing(
                            ProfileActionFeedItemResponse::deadline,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                            ProfileActionFeedItemResponse::createdAt,
                            Comparator.reverseOrder())
                    .thenComparing(
                            ProfileActionFeedItemResponse::id,
                            Comparator.reverseOrder());

    private final ProfileService profiles;
    private final EmailActionRepository actions;
    private final CategoryScoreService categoryScores;
    private final ActionDeadlinePresentationService deadlinePresentation;

    public ProfileActionFeedService(
            ProfileService profiles,
            EmailActionRepository actions,
            CategoryScoreService categoryScores,
            ActionDeadlinePresentationService deadlinePresentation) {
        this.profiles = profiles;
        this.actions = actions;
        this.categoryScores = categoryScores;
        this.deadlinePresentation = deadlinePresentation;
    }

    public List<ProfileActionFeedItemResponse> feed(long profileId) {
        ProfileResponse profile = profiles.get(profileId);
        var feed = new ArrayList<ProfileActionFeedItemResponse>();
        for (EmailAction action : actions.findAllByStatus(EmailActionStatus.PENDING)) {
            var presentation = deadlinePresentation.present(action.getStatus(), action.getDeadline());
            if (presentation.effectiveStatus() == EmailActionEffectiveStatus.PENDING) {
                feed.add(feedItem(profile, action, presentation));
            }
        }
        return feed.stream().sorted(RANKING_ORDER).toList();
    }

    private ProfileActionFeedItemResponse feedItem(
            ProfileResponse profile,
            EmailAction action,
            ActionDeadlinePresentation presentation) {
        var email = action.getEmail();
        EmailCategory category = email.getCategory();
        var confidence = email.getClassificationConfidence();
        if (category == null || confidence == null) {
            throw new IncompleteEmailClassificationException();
        }
        double categoryScore = categoryScores.score(profile, categoryKey(category)).score();
        int relevanceScore = (int) Math.round(
                categoryScore * confidence.persistedValue() * 100);
        ActionRelevance relevance = relevance(relevanceScore);
        int rankingScore = relevanceScore + urgencyWeight(presentation.urgency());
        return new ProfileActionFeedItemResponse(
                action.getId(),
                email.getId(),
                email.getSubject(),
                category,
                confidence,
                action.getActionType(),
                action.getDeadline(),
                action.getActionUrl(),
                presentation.effectiveStatus(),
                presentation.urgency(),
                presentation.daysRemaining(),
                relevance,
                relevanceScore,
                rankingScore,
                relevance + " relevance + " + presentation.urgency(),
                action.getCreatedAt());
    }

    private static String categoryKey(EmailCategory category) {
        return category.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static ActionRelevance relevance(int score) {
        if (score >= HIGH_RELEVANCE_MIN) {
            return ActionRelevance.HIGH;
        }
        return score >= MEDIUM_RELEVANCE_MIN ? ActionRelevance.MEDIUM : ActionRelevance.LOW;
    }

    private static int urgencyWeight(ActionUrgency urgency) {
        return switch (urgency) {
            case DUE_TODAY -> DUE_TODAY_WEIGHT;
            case DUE_SOON -> DUE_SOON_WEIGHT;
            case UPCOMING -> UPCOMING_WEIGHT;
            case NO_DEADLINE -> 0;
            case COMPLETED, NOT_INTERESTED, MISSED ->
                    throw new IllegalArgumentException("Terminal urgency is not actionable");
        };
    }
}
