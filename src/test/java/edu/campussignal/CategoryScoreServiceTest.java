package edu.campussignal;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import edu.campussignal.config.CategoryScoreProperties;
import edu.campussignal.dto.CategoryScoreResponse.Components;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.service.CategoryScoreService;
import static org.assertj.core.api.Assertions.*;

class CategoryScoreServiceTest {
    private final CategoryScoreService scores = new CategoryScoreService(
            new CategoryScoreProperties(0.40, 0.15, 0.35, 0.10, 0.5, 0));

    private ProfileResponse profile(int year, String programme, String department, List<String> interests) {
        return new ProfileResponse(1L, "student@example.edu", department, programme, year, null,
                interests, Instant.parse("2026-09-08T00:00:00Z"));
    }

    @Test
    void twoUsersHaveDifferentScoresForTheSameCategory() {
        var userA = profile(3, "BTech", "Computer Science", List.of("internships", "research", "hackathons"));
        var userB = profile(3, "BTech", "Computer Science", List.of("sports", "cultural events", "clubs"));
        var scoreA = scores.score(userA, "internships");
        var scoreB = scores.score(userB, "internships");
        assertThat(scoreA.score()).isCloseTo(0.6875, within(1e-10));
        assertThat(scoreB.score()).isCloseTo(0.2875, within(1e-10));
        assertThat(scoreA.score() - scoreB.score()).isCloseTo(0.40, within(1e-10));
        assertThat(scoreA.components().explicitPreference()).isOne();
        assertThat(scoreB.components().explicitPreference()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"internships", "research", "hackathons", "sports", "new category"})
    void scoresAndComponentsStayNormalized(String category) {
        for (int year : new int[]{1, 2, 4, 9}) {
            for (double behavior : new double[]{0, 0.5, 1}) {
                var result = scores.score(profile(year, "BTech", "Computer Science", List.of(category)),
                        category, behavior);
                assertThat(result.score()).isBetween(0.0, 1.0);
                assertThat(result.components().profileRelevance()).isBetween(0.0, 1.0);
                assertThat(result.components().explicitPreference()).isBetween(0.0, 1.0);
                assertThat(result.components().behavioralPreference()).isBetween(0.0, 1.0);
                assertThat(result.components().relatedInterestSimilarity()).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void yearProgrammeAndDepartmentAffectProfileRelevance() {
        var junior = profile(1, "BA", "History", List.of("music"));
        var senior = profile(4, "BTech", "Computer Science", List.of("music"));
        var researcher = profile(1, "PhD", "History", List.of("music"));
        assertThat(scores.score(senior, "internships").score()).isGreaterThan(scores.score(junior, "internships").score());
        assertThat(scores.score(researcher, "research").score()).isGreaterThan(scores.score(junior, "research").score());
        assertThat(scores.score(senior, "hackathons").score()).isGreaterThan(scores.score(junior, "hackathons").score());
    }

    @Test
    void missingHistoryAndRelatedSimilarityUseExplicitColdStartDefaults() {
        var result = scores.score(profile(2, "BA", "History", List.of("sports")), "internships", null);
        assertThat(result.components().behavioralPreference()).isEqualTo(0.5);
        assertThat(result.components().relatedInterestSimilarity()).isZero();
    }

    @Test
    void customWeightsAndDefaultsAreUsed() {
        var custom = new CategoryScoreService(new CategoryScoreProperties(0.1, 0.2, 0.3, 0.4, 0.6, 0.2));
        var result = custom.score(profile(2, "BTech", "Computer Science", List.of("internships")), "internships");
        assertThat(result.score()).isCloseTo(0.1 + 0.2 * 0.5 + 0.3 * 0.6 + 0.4 * 0.2, within(1e-10));
    }

    @Test
    void rejectsInvalidWeightsAndComponents() {
        assertThatThrownBy(() -> new CategoryScoreProperties(1, 1, 1, 1, 0.5, 0))
                .isInstanceOf(IllegalArgumentException.class);
        for (double value : new double[]{-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> new Components(0, 0, value, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CategoryScoreProperties(0.4, 0.15, 0.35, 0.1, value, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void categoryNormalizationAndUnknownCategoriesRemainExtensible() {
        var user = profile(1, "BA", "History", List.of("new category"));
        var result = scores.score(user, " NEW CATEGORY ");
        assertThat(result.category()).isEqualTo("new category");
        assertThat(result.components().explicitPreference()).isOne();
        assertThat(result.components().profileRelevance()).isEqualTo(0.5);
        assertThatThrownBy(() -> scores.score(user, " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
