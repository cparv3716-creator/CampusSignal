package edu.campussignal;

import java.time.Instant;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionType;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.dto.StudentProfileRequest;
import edu.campussignal.entity.Email;
import edu.campussignal.entity.UserAccount;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.repository.UserAccountRepository;
import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.repository.UserSessionRepository;
import edu.campussignal.security.AuthenticatedUserService;
import edu.campussignal.service.EmailActionService;
import edu.campussignal.service.ProfileService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest(properties = "campussignal.auth.required=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentIsolationApiTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired EmailRepository emails;
    @Autowired EmailActionRepository actions;
    @Autowired UserSessionRepository sessions;
    @Autowired AuthenticatedUserService auth;
    @Autowired ProfileService profileService;
    @Autowired EmailActionService actionService;

    private UserAccount studentA;
    private UserAccount studentB;
    private AuthenticatedUserService.Grant sessionA;
    private AuthenticatedUserService.Grant sessionB;
    private Email emailA;

    @BeforeEach
    void arrangeStudents() {
        actions.deleteAll(); emails.deleteAll(); profiles.deleteAll(); sessions.deleteAll(); users.deleteAll();
        studentA = users.saveAndFlush(new UserAccount("google-subject-a", "a@example.test", "Student A"));
        studentB = users.saveAndFlush(new UserAccount("google-subject-b", "b@example.test", "Student B"));
        profileService.saveForUser(studentA, profile("Student A", "internships", "hackathons"));
        profileService.saveForUser(studentB, profile("Student B", "research opportunities", "chess"));
        emailA = email(studentA, "a-message", "Internship application deadline", EmailCategory.CAREER);
        email(studentB, "b-message", "Research workshop for chess club", EmailCategory.ACADEMIC_RESEARCH);
        actionService.create(studentA, emailA.getId(), new EmailActionCreateRequest(
                EmailActionType.APPLY, null, "https://example.test/apply"));
        sessionA = auth.create(studentA); sessionB = auth.create(studentB);
    }

    @Test
    void studentCannotReadOtherStudentsProfileEmailsRankingOrActions() throws Exception {
        mvc.perform(get("/api/v1/me/profile").cookie(cookie(sessionB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("b@example.test"));
        mvc.perform(get("/api/v1/me/emails").cookie(cookie(sessionB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subject").value("Research workshop for chess club"));
        mvc.perform(get("/api/v1/me/email-feed").cookie(cookie(sessionB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].emailSubject").value("Research workshop for chess club"));
        mvc.perform(get("/api/v1/me/actions").cookie(cookie(sessionB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        // Historical ID-addressable routes are unavailable in multi-user mode.
        mvc.perform(get("/api/v1/emails/{id}", emailA.getId()).cookie(cookie(sessionB)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/me/emails/{id}/actions", emailA.getId())
                        .cookie(cookie(sessionB)).header("X-CampusSignal-CSRF", sessionB.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actionType\":\"APPLY\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentCanSaveMultipleInterestsAndRestorePrivateDashboard() throws Exception {
        mvc.perform(put("/api/v1/me/profile").cookie(cookie(sessionA))
                        .header("X-CampusSignal-CSRF", sessionA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Student A","college":"Campus University","department":"Computing",
                                "programme":"BTech","year":3,"graduationYear":2028,"semester":5,
                                "interests":["internships","hackathons","scholarships"]}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.interests.length()").value(3));
        mvc.perform(get("/api/v1/me/profile").cookie(cookie(sessionA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("a@example.test"))
                .andExpect(jsonPath("$.interests[1]").value("hackathons"));
        mvc.perform(get("/api/v1/me/email-feed").cookie(cookie(sessionA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].emailSubject").value("Internship application deadline"))
                .andExpect(jsonPath("$[0].rankingExplanation")
                        .value(containsString("Career or placement announcement.")))
                .andExpect(jsonPath("$[0].rankingExplanation")
                        .value(not(containsString(" relevance +"))));
    }

    private StudentProfileRequest profile(String name, String... interests) {
        return new StudentProfileRequest(name, "Campus University", "Computing", "BTech", 3, 2028, 5,
                java.util.List.of(interests));
    }
    private Email email(UserAccount owner, String id, String subject, EmailCategory category) {
        return emails.saveAndFlush(Email.classified(owner, new IncomingEmail(id, subject,
                "notices@example.test", Instant.parse("2026-11-05T08:00:00Z"), "Synthetic private email body"),
                new EmailClassificationResult(category, ClassificationConfidence.HIGH)));
    }
    private Cookie cookie(AuthenticatedUserService.Grant grant) {
        return new Cookie(AuthenticatedUserService.SESSION_COOKIE, grant.sessionId());
    }
}
