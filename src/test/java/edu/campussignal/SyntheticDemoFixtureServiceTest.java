package edu.campussignal;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.service.ProfileActionFeedService;
import edu.campussignal.demo.SyntheticDemoFixtureService;

@SpringBootTest
class SyntheticDemoFixtureServiceTest {
    @Autowired SyntheticDemoFixtureService fixture;
    @Autowired EmailRepository emails;
    @Autowired EmailActionRepository actions;
    @Autowired UserProfileRepository profiles;
    @Autowired ProfileActionFeedService feeds;
    @Autowired Clock clock;

    @BeforeEach
    void clearState() {
        actions.deleteAll();
        emails.deleteAll();
        profiles.deleteAll();
    }

    @Test
    void seedsClassifiedEmailsActionsAndFeedableFutureDeadlines() {
        var seededToday = java.time.LocalDate.now(clock);
        var result = fixture.seed();

        assertThat(result.totalEmails()).isEqualTo(6);
        assertThat(result.createdEmails()).isEqualTo(6);
        assertThat(result.createdActions()).isGreaterThanOrEqualTo(3);
        assertThat(emails.count()).isEqualTo(6);
        assertThat(actions.count()).isGreaterThanOrEqualTo(3);
        assertThat(actions.count()).isEqualTo(5);
        assertThat(actions.findAll().stream().map(action -> action.getDeadline()).toList())
                .containsExactlyInAnyOrder(
                        seededToday.plusDays(14),
                        seededToday.plusDays(21),
                        seededToday.plusDays(10),
                        seededToday.plusDays(7),
                        seededToday.plusDays(28));

        var categories = emails.findAll().stream().map(email -> email.getCategory().name()).toList();
        assertThat(categories).contains("CAREER", "ACADEMIC_RESEARCH", "EVENT_ACTIVITY");
        var feed = feeds.feed(result.profileId());
        assertThat(feed).hasSizeGreaterThanOrEqualTo(3);
        assertThat(feed.stream().filter(item -> item.relevance().name().equals("LOW")).count())
                .isLessThan(5);
    }

    @Test
    void rerunningFixtureIsIdempotent() {
        var first = fixture.seed();
        var second = fixture.seed();

        assertThat(second.createdEmails()).isZero();
        assertThat(second.createdActions()).isZero();
        assertThat(emails.count()).isEqualTo(first.totalEmails());
        assertThat(actions.count()).isEqualTo(first.createdActions());
        assertThat(profiles.count()).isEqualTo(1);
        assertThat(feeds.feed(first.profileId())).hasSizeGreaterThanOrEqualTo(3);
    }
}
