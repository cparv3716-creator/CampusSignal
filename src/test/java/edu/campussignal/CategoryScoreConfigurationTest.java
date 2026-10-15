package edu.campussignal;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import edu.campussignal.config.CategoryScoreProperties;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.service.CategoryScoreService;
import static org.assertj.core.api.Assertions.*;

class CategoryScoreConfigurationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CategoryScoreProperties.class)
    static class ScoreConfiguration {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ScoreConfiguration.class);

    @Test
    void externalPropertiesConfigureTheActualCalculation() {
        runner.withPropertyValues("category-score.explicit-weight=1", "category-score.profile-weight=0",
                        "category-score.behavioral-weight=0", "category-score.related-weight=0")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var service = new CategoryScoreService(context.getBean(CategoryScoreProperties.class));
                    var user = new ProfileResponse(1L, "student@example.edu", "Arts", "BA", 1,
                            null, List.of("internships"), Instant.now());
                    assertThat(service.score(user, "internships").score()).isOne();
                    assertThat(service.score(user, "sports").score()).isZero();
                });
    }

    @Test
    void invalidConfigurationFailsAtStartup() {
        runner.withPropertyValues("category-score.explicit-weight=0.9")
                .run(context -> assertThat(context).hasFailed());
    }
}
