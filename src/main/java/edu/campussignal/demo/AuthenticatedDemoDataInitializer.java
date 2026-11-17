package edu.campussignal.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionType;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.dto.StudentProfileRequest;
import edu.campussignal.entity.UserAccount;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.UserAccountRepository;
import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.service.EmailActionService;
import edu.campussignal.service.EmailIngestionService;
import edu.campussignal.service.ProfileService;

/** Owner-scoped fictional records used only by the self-contained demo profile. */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "campussignal.demo-data.enabled", havingValue = "true")
public class AuthenticatedDemoDataInitializer implements ApplicationRunner {
    public static final String DEMO_GOOGLE_SUBJECT = "campussignal-fictional-demo-user";
    public static final String DEMO_EMAIL = "demo.student@demo.example.com";

    private final UserAccountRepository users;
    private final UserProfileRepository profiles;
    private final ProfileService profileService;
    private final EmailIngestionService ingestion;
    private final EmailActionService actionService;
    private final EmailActionRepository actions;
    private final Clock clock;

    public AuthenticatedDemoDataInitializer(
            UserAccountRepository users,
            UserProfileRepository profiles,
            ProfileService profileService,
            EmailIngestionService ingestion,
            EmailActionService actionService,
            EmailActionRepository actions,
            Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.profileService = profileService;
        this.ingestion = ingestion;
        this.actionService = actionService;
        this.actions = actions;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        UserAccount user = users.findByGoogleSubject(DEMO_GOOGLE_SUBJECT)
                .orElseGet(() -> users.saveAndFlush(
                        new UserAccount(DEMO_GOOGLE_SUBJECT, DEMO_EMAIL, "Demo Student")));
        if (profiles.findByUserId(user.getId()).isEmpty()) {
            profileService.saveForUser(user, new StudentProfileRequest(
                    "Demo Student",
                    "CampusSignal Demo University",
                    "Computer Science",
                    "BTech",
                    3,
                    LocalDate.now(clock).getYear() + 1,
                    5,
                    List.of("internships", "research", "hackathons")));
        }

        LocalDate today = LocalDate.now(clock);
        seedAction(
                user,
                "authenticated-demo-career-internship",
                "Summer Internship Opportunity - Robotics",
                "career.office@demo.example.com",
                "Apply for the fictional robotics internship at "
                        + "https://careers.demo.example.com/apply.",
                EmailActionType.APPLY,
                today.plusDays(3),
                "https://careers.demo.example.com/apply");
        seedAction(
                user,
                "authenticated-demo-campus-hackathon",
                "Campus Hackathon Registration",
                "events.team@demo.example.com",
                "Register for the fictional student hackathon at "
                        + "https://events.demo.example.com/register.",
                EmailActionType.REGISTER,
                today.plusDays(10),
                "https://events.demo.example.com/register");
    }

    private void seedAction(
            UserAccount user,
            String messageId,
            String subject,
            String sender,
            String body,
            EmailActionType actionType,
            LocalDate deadline,
            String actionUrl) {
        long emailId = ingestion.ingest(user, new IncomingEmail(
                messageId, subject, sender, Instant.now(clock).minusSeconds(3600), body)).id();
        if (!actions.existsByEmailId(emailId)) {
            actionService.create(user, emailId,
                    new EmailActionCreateRequest(actionType, deadline, actionUrl));
        }
    }
}
