package edu.campussignal;

import org.junit.jupiter.api.Test;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.service.EmailClassificationService;
import edu.campussignal.service.RuleBasedEmailClassificationService;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedEmailClassificationServiceTest {
    private final EmailClassificationService classifier = new RuleBasedEmailClassificationService();

    @Test
    void academicSubject() {
        String subject = "Department Seminar: Reliable Systems";
        String body = "Speaker: Dr Example. Abstract: dependable distributed systems. Date: Friday.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void careerSubject() {
        String subject = "Summer Internship Opportunity — Robotics";
        String body = "The employer offers a stipend. Applicants must meet the stated eligibility criteria.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void eventSubject() {
        String subject = "Film Screening | Friday | 7 PM | Hall A";
        String body = "Students are invited to attend the screening at the announced venue.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.EVENT_ACTIVITY);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void callSubject() {
        String subject = "Call for Club Coordinators";
        String body = "Eligible students may apply using the form before Friday's application deadline.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CALL_FOR_PARTICIPATION);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void adminSubject() {
        String subject = "Circular: Laboratory Access Protocol";
        String body = "All students must follow these mandatory access instructions from Monday.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void wellbeingSubject() {
        String subject = "Counselling and Mental-Health Support";
        String body = "Counsellors are available for student appointments from 10 AM to 6 PM.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.WELLBEING_SUPPORT);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void recognitionSubject() {
        String subject = "Student team wins Best Design Award";
        String body = "The team received first place in the completed inter-university competition.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.RECOGNITION_ACHIEVEMENT);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void generalSubject() {
        String subject = "Hostel Bicycle Auction";
        String body = "Bidding starts at noon and successful bidders may collect bicycles afterward.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.GENERAL_OTHER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void opportunityNotCareer() {
        String subject = "Opportunity to attend a film screening";
        String body = "The screening is on Friday at 7 PM in Hall A.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.EVENT_ACTIVITY);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void researchInJob() {
        String subject = "Graduate analyst role";
        String body = "Research skills are preferred. Salary details are listed; submit a resume and application.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void prizeNotRecognition() {
        String subject = "Hackathon registrations — prize pool available";
        String body = "Register your team for the future contest before the submission deadline.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CALL_FOR_PARTICIPATION);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void careerVsEvent() {
        String subject = "Pre-placement talk";
        String body = "The talk is Friday in Hall B. A graduate role is open and applications require a resume.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void academicVsEvent() {
        String subject = "Research seminar";
        String body = "The speaker will present a scholarly abstract on Friday at 3 PM in Seminar Hall.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void callVsEvent() {
        String subject = "Auditions Friday";
        String body = "Selection criteria are in the application form. Auditions take place in the activity hall.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CALL_FOR_PARTICIPATION);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void quoteMisleading() {
        String subject = "Re: Pool status";
        String body = "Pool reopens today.\n\nOn Thursday, a previous sender wrote:\n> Internship applications open.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void quoteOnlyRescue() {
        String subject = "Re: Reminder";
        String body = "Deadline today.\n\nOn Thursday, a previous sender wrote:\n"
                + "> Graduate software engineer job announcement. Submit a resume and application.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void emptyBoth() {
        String subject = null;
        String body = null;

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.GENERAL_OTHER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.LOW);
    }

    @Test
    void emptySubject() {
        String subject = "";
        String body = "Counselling appointments are available for students.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.WELLBEING_SUPPORT);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void emptyBody() {
        String subject = "Summer internship opportunity";
        String body = "";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void unicodeHyphen() {
        String subject = "Pre‑placement graduate‑role";
        String body = "The employer is hiring graduates; role eligibility and application details follow.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }
}
