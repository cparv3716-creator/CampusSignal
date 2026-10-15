package edu.campussignal.service;

import java.util.Locale;
import org.springframework.stereotype.Service;
import edu.campussignal.config.CategoryScoreProperties;
import edu.campussignal.dto.CategoryScoreResponse;
import edu.campussignal.dto.CategoryScoreResponse.Components;
import edu.campussignal.dto.ProfileResponse;

@Service
public class CategoryScoreService {
    private final CategoryScoreProperties settings;

    public CategoryScoreService(CategoryScoreProperties settings) { this.settings = settings; }

    public CategoryScoreResponse score(ProfileResponse profile, String category) {
        return score(profile, category, null);
    }

    /** Null history deliberately means the configured neutral cold-start value, not learned behavior. */
    public CategoryScoreResponse score(ProfileResponse profile, String category, Double behavioralPreference) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category must not be blank");
        }
        String normalized = category.strip().toLowerCase(Locale.ROOT);
        double explicit = profile.interests().contains(normalized) ? 1.0 : 0.0;
        var components = new Components(explicit, profileRelevance(profile, normalized),
                behavioralPreference == null ? settings.initialBehavioral() : behavioralPreference,
                settings.initialRelated());
        double score = settings.explicitWeight() * components.explicitPreference()
                + settings.profileWeight() * components.profileRelevance()
                + settings.behavioralWeight() * components.behavioralPreference()
                + settings.relatedWeight() * components.relatedInterestSimilarity();
        return new CategoryScoreResponse(normalized, Math.clamp(score, 0.0, 1.0), components);
    }

    private double profileRelevance(ProfileResponse profile, String category) {
        double progression = Math.clamp(profile.year() / 4.0, 0.0, 1.0);
        String programme = profile.programme().toLowerCase(Locale.ROOT);
        String department = profile.department().toLowerCase(Locale.ROOT);
        return switch (category) {
            case "internships" -> progression;
            case "research" -> programme.contains("phd") || programme.contains("master")
                    || programme.contains("mtech") || programme.contains("msc") ? 1.0 : 0.6 * progression;
            case "hackathons" -> department.contains("computer") || department.contains("engineering")
                    || programme.contains("btech") ? 1.0 : 0.3;
            default -> 0.5; // Unknown categories stay extensible with neutral profile relevance.
        };
    }
}
