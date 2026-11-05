package edu.campussignal.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.dto.ProfileCreateRequest;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.service.DeterministicEmailActionExtractor;
import edu.campussignal.service.EmailActionService;
import edu.campussignal.service.EmailIngestionService;
import edu.campussignal.service.ProfileNotFoundException;
import edu.campussignal.service.ProfileService;

@Service
public class SyntheticDemoFixtureService {
    public static final String PROFILE_EMAIL = "demo.student@demo.example.com";

    private final EmailIngestionService ingestion;
    private final DeterministicEmailActionExtractor extractor;
    private final EmailActionService actions;
    private final ProfileService profiles;
    private final Clock clock;

    public SyntheticDemoFixtureService(
            EmailIngestionService ingestion,
            DeterministicEmailActionExtractor extractor,
            EmailActionService actions,
            ProfileService profiles,
            Clock clock) {
        this.ingestion = ingestion;
        this.extractor = extractor;
        this.actions = actions;
        this.profiles = profiles;
        this.clock = clock;
    }

    public FixtureResult seed() {
        LocalDate today = LocalDate.now(clock);
        List<DemoEmail> emails = List.of(
                new DemoEmail("campussignal-demo-internship-v1",
                        "Career Office internship applications",
                        "internships@demo.example.com",
                        "Internship applications close on " + today.plusDays(14)
                                + ". Apply here: https://demo.example.com/internships/apply"),
                new DemoEmail("campussignal-demo-scholarship-v1",
                        "Merit scholarship submission window",
                        "scholarships@demo.example.com",
                        "Submit your scholarship form by " + today.plusDays(21)
                                + " at https://demo.example.com/scholarships/submit."),
                new DemoEmail("campussignal-demo-workshop-v1",
                        "Research workshop registration",
                        "workshops@demo.example.com",
                        "Register by " + today.plusDays(10)
                                + " at https://demo.example.com/workshops/register."),
                new DemoEmail("campussignal-demo-academic-v1",
                        "Course project upload reminder",
                        "academics@demo.example.com",
                        "Upload the course project by " + today.plusDays(7)
                                + " at https://demo.example.com/academics/upload."),
                new DemoEmail("campussignal-demo-event-v1",
                        "Campus dance activity Friday",
                        "events@demo.example.com",
                        "Please attend the campus dance activity in the main hall on "
                                + today.plusDays(28) + ". Details: "
                                + "https://demo.example.com/events/cultural-evening."),
                new DemoEmail("campussignal-demo-announcement-v1",
                        "Campus library opening hours",
                        "announcements@demo.example.com",
                        "The library will remain open during the examination period. "
                                + "This is an informational announcement for students."));

        int createdEmails = 0;
        int createdActions = 0;
        for (DemoEmail demoEmail : emails) {
            var result = ingestion.ingest(new IncomingEmail(
                    demoEmail.messageId(), demoEmail.subject(), demoEmail.sender(),
                    Instant.now(clock), demoEmail.body()));
            if (result.created()) {
                createdEmails++;
            }
            var candidate = extractor.extract(demoEmail.subject(), demoEmail.body());
            if (candidate.isPresent() && !hasAction(result.id())) {
                var value = candidate.orElseThrow();
                actions.create(result.id(), new EmailActionCreateRequest(
                        value.actionType(), value.deadline(), value.actionUrl()));
                createdActions++;
            }
        }

        ProfileResponse profile = findOrCreateProfile();
        return new FixtureResult(emails.size(), createdEmails, createdActions, profile.id());
    }

    private boolean hasAction(long emailId) {
        return actions.listForEmail(emailId).stream().findAny().isPresent();
    }

    private ProfileResponse findOrCreateProfile() {
        try {
            return profiles.getByEmail(PROFILE_EMAIL);
        } catch (ProfileNotFoundException exception) {
            return profiles.create(new ProfileCreateRequest(
                    PROFILE_EMAIL,
                    "Synthetic Studies",
                    "Demo Programme",
                    2,
                    4,
                    List.of("internships", "scholarships", "research", "events")));
        }
    }

    public record FixtureResult(int totalEmails, int createdEmails, int createdActions, long profileId) {}

    private record DemoEmail(String messageId, String subject, String sender, String body) {}
}
