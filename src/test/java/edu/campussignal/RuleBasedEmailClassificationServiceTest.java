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
    void namedActivityWithSubjectScheduleIsAnEvent() {
        String subject = "Join the campus dance — Friday 7 PM — Hall A";
        String body = "";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.EVENT_ACTIVITY);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
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

    @Test
    void replyPrefixNormalization() {
        String subject = "Re: Department Seminar: Reliable Systems";
        String body = "Speaker: Dr Example. Abstract: dependable systems. Date: Friday.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void forwardPrefixNormalization() {
        String subject = "Fwd: Summer Internship Opportunity — Robotics";
        String body = "The employer describes the role and its eligibility requirements.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void signatureKeywordIgnored() {
        String subject = "Pool reopened";
        String body = "The pool reopens today and normal facility operation resumes.\n\n"
                + "--\nCareer Office volunteer";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void footerKeywordIgnored() {
        String subject = "Service unavailable";
        String body = "The campus network service is unavailable during maintenance.\n\n"
                + "Mailing-list footer\nUpcoming research events and opportunities";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void urlKeywordIgnored() {
        String subject = "Facility feedback";
        String body = "Please comment on the operating hours and service quality of the campus facility: "
                + "https://career.example/survey";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void emailAddressKeywordIgnored() {
        String subject = "Holiday circular";
        String body = "Students must follow the official holiday policy. Contact research-office@example.edu.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void negatedInternship() {
        String subject = "Volunteer project";
        String body = "This is not an internship or job. Applications are open for student volunteers.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CALL_FOR_PARTICIPATION);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void subjectNegationSuppressesCareerCueWithoutHidingAcademicPurpose() {
        String subject = "Not an internship — research seminar";
        String body = "A speaker will present a scholarly lecture and abstract.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void negatedEvent() {
        String subject = "Online academic resource";
        String body = "This is not an event. Course materials and lecture resources are available asynchronously.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void futureAwardNotRecognition() {
        String subject = "Applications open for Student Excellence Award";
        String body = "Eligible students may submit an application before the deadline for the future award process.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CALL_FOR_PARTICIPATION);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void exactTieFallsBackToGeneral() {
        String subject = "Research seminar and film screening";
        String body = "This message gives equal prominence to two separate Friday activities: "
                + "a research seminar with a speaker and abstract, and a film screening in Hall A. "
                + "Students may attend either activity.";

        // The two independent purposes have equally strong subject/body evidence.
        // Neither activity is presented as the primary purpose of this combined message.
        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.GENERAL_OTHER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.LOW);
    }

    @Test
    void repeatedQuotedEvidenceDoesNotBoost() {
        String subject = "Update";
        String body = "Students are invited to an exhibition on Friday in Hall A.\n\n"
                + "On Thursday, a previous sender wrote:\n"
                + "> internship internship internship internship internship internship";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.EVENT_ACTIVITY);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void repeatedCurrentKeywordDoesNotArtificiallyBoost() {
        String subject = "Opportunity opportunity opportunity";
        String body = "Opportunity opportunity opportunity. Thank you for reading this update.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.GENERAL_OTHER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.LOW);
    }

    @Test
    void htmlBodyClassification() {
        String subject = "Seminar";
        String body = "<div>Research seminar<br>Speaker: Dr Example<br>"
                + "Abstract: reliable systems<br>Date: Friday</div>";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void htmlEntitiesDecoded() {
        String subject = "Research &amp; Innovation Seminar";
        String body = "<p>Speaker: Dr Example<br>Abstract: responsible innovation<br>Date: Monday</p>";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void facilityFeedbackIsAdministrative() {
        String subject = "Campus Dining Service Feedback";
        String body = "Students are asked to comment on the operation, timings, and quality of this institute facility.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
    }

    @Test
    void genericSurveyIsNotAdministrative() {
        String subject = "Student Opinion Survey";
        String body = "A campus club invites students to complete a general opinion questionnaire.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.GENERAL_OTHER);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.LOW);
    }

    @Test
    void nonSelectiveAcademicRegistration() {
        String subject = "Registration Open: Introduction to Data Analysis";
        String body = "This academic course workshop covers data analysis learning outcomes and meets every Friday. "
                + "All students may register; there is no selection process.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void selectiveAcademicApplication() {
        String subject = "Applications Open: Summer Research Programme";
        String body = "The programme has limited seats, eligibility rules, a selection process, and a Friday deadline.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.CALL_FOR_PARTICIPATION);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void publicYogaActivityIsEvent() {
        String subject = "Yoga for Wellness — Saturday Morning";
        String body = "Join this one-hour public campus activity at 8 AM on Saturday in Hall C. "
                + "No counselling or support-service access is provided.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.EVENT_ACTIVITY);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }

    @Test
    void wellbeingServiceEventIsWellbeing() {
        String subject = "Counselling Access Day";
        String body = "Licensed counsellors provide walk-in appointments and mental-health support "
                + "from 10 AM in Hall D.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.WELLBEING_SUPPORT);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void repeatedPrefixesAndUnicodeSpacingNormalizeTogether() {
        String subject = "Re: FWD: Re: Research\u00a0Workshop — Reliable Systems";
        String body = "Speaker: Dr Example. Abstract: dependable systems.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ACADEMIC_RESEARCH);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void forwardedMessageBoundaryKeepsQuotedEvidenceOutOfCurrentMessage() {
        String subject = "Pool status";
        String body = "The pool reopens today and normal facility service resumes.\n\n"
                + "----- Forwarded message -----\n"
                + "Summer internship applications are open with a stipend.";

        EmailClassificationResult result = classifier.classify(subject, body);

        assertThat(result.category()).isEqualTo(EmailCategory.ADMINISTRATIVE_NOTICE);
        assertThat(result.confidence()).isEqualTo(ClassificationConfidence.HIGH);
    }

    @Test
    void nearbyNegationWindowIncludesExactlyFourPrecedingTokens() {
        EmailClassificationResult fourTokens = classifier.classify(
                "Volunteer project", "This is not a current paid internship.");
        EmailClassificationResult fiveTokens = classifier.classify(
                "Volunteer project", "This is not actually a current paid internship.");

        assertThat(fourTokens.category()).isEqualTo(EmailCategory.GENERAL_OTHER);
        assertThat(fourTokens.confidence()).isEqualTo(ClassificationConfidence.LOW);
        assertThat(fiveTokens.category()).isEqualTo(EmailCategory.CAREER);
        assertThat(fiveTokens.confidence()).isEqualTo(ClassificationConfidence.MEDIUM);
    }
}
