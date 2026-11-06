package edu.campussignal.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionType;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.dto.ProfileCreateRequest;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.service.EmailActionService;
import edu.campussignal.service.EmailIngestionService;
import edu.campussignal.service.ProfileService;

/** Opt-in fictional fixtures for deployments that must not access a real Gmail account. */
@Component
@ConditionalOnProperty(name = "campussignal.demo-data.enabled", havingValue = "true")
public class SyntheticDemoDataInitializer implements ApplicationRunner {
    public static final String DEMO_PROFILE_EMAIL = "student@demo.example.com";

    private final EmailIngestionService ingestion;
    private final EmailActionService actionService;
    private final ProfileService profileService;
    private final EmailActionRepository actions;
    private final UserProfileRepository profiles;
    private final Clock clock;

    public SyntheticDemoDataInitializer(
            EmailIngestionService ingestion,
            EmailActionService actionService,
            ProfileService profileService,
            EmailActionRepository actions,
            UserProfileRepository profiles,
            Clock clock) {
        this.ingestion = ingestion;
        this.actionService = actionService;
        this.profileService = profileService;
        this.actions = actions;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        profiles.findByEmail(DEMO_PROFILE_EMAIL).orElseGet(() -> {
            profileService.create(new ProfileCreateRequest(
                    DEMO_PROFILE_EMAIL,
                    "Computer Science",
                    "BTech",
                    3,
                    5,
                    List.of("internships", "research", "hackathons")));
            return profiles.findByEmail(DEMO_PROFILE_EMAIL).orElseThrow();
        });

        LocalDate today = LocalDate.now(clock);
        seedAction(
                "demo-career-internship",
                "Summer Internship Opportunity - Robotics",
                "career.office@demo.example.com",
                "The employer offers a stipend. Apply for the robotics internship at "
                        + "https://careers.demo.example.com/apply before the stated deadline.",
                EmailActionType.APPLY,
                today.plusDays(3),
                "https://careers.demo.example.com/apply");
        seedAction(
                "demo-campus-hackathon",
                "Campus Hackathon Registration",
                "events.team@demo.example.com",
                "Register for the student hackathon at "
                        + "https://events.demo.example.com/register. Teams are welcome.",
                EmailActionType.REGISTER,
                today.plusDays(10),
                "https://events.demo.example.com/register");
    }

    private void seedAction(
            String messageId,
            String subject,
            String sender,
            String body,
            EmailActionType actionType,
            LocalDate deadline,
            String actionUrl) {
        Instant receivedAt = Instant.now(clock).minusSeconds(3600);
        long emailId = ingestion.ingest(
                new IncomingEmail(messageId, subject, sender, receivedAt, body)).id();
        if (!actions.existsByEmailId(emailId)) {
            actionService.create(
                    emailId,
                    new EmailActionCreateRequest(actionType, deadline, actionUrl));
        }
    }
}
