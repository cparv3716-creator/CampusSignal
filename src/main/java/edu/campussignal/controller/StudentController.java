package edu.campussignal.controller;

import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionResponse;
import edu.campussignal.dto.EmailActionStatusRequest;
import edu.campussignal.dto.EmailContentResponse;
import edu.campussignal.dto.EmailResponse;
import edu.campussignal.dto.RankedEmailFeedItemResponse;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.dto.StudentProfileRequest;
import edu.campussignal.dto.StudentProfileResponse;
import edu.campussignal.entity.UserAccount;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.security.AuthenticatedUserService;
import edu.campussignal.service.EmailActionCandidateService;
import edu.campussignal.service.EmailActionService;
import edu.campussignal.service.EmailReadService;
import edu.campussignal.service.ProfileEmailFeedService;
import edu.campussignal.service.ProfileService;
import edu.campussignal.service.StudentGmailSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class StudentController {
    private final AuthenticatedUserService auth; private final ProfileService profiles; private final EmailReadService emails; private final ProfileEmailFeedService feeds; private final StudentGmailSyncService gmail; private final EmailActionService actions; private final EmailActionCandidateService candidates; private final EmailRepository emailRepository;
    public StudentController(AuthenticatedUserService auth, ProfileService profiles, EmailReadService emails, ProfileEmailFeedService feeds, StudentGmailSyncService gmail, EmailActionService actions, EmailActionCandidateService candidates, EmailRepository emailRepository) { this.auth = auth; this.profiles = profiles; this.emails = emails; this.feeds = feeds; this.gmail = gmail; this.actions = actions; this.candidates = candidates; this.emailRepository = emailRepository; }
    @GetMapping("/profile") public StudentProfileResponse profile(HttpServletRequest request) { return profiles.getForUser(user(request)); }
    @PutMapping("/profile") public StudentProfileResponse saveProfile(@RequestHeader(value="X-CampusSignal-CSRF", required=false) String csrf, HttpServletRequest request, @Valid @RequestBody StudentProfileRequest body) { return profiles.saveForUser(mutate(request, csrf), body); }
    @GetMapping("/emails") public List<EmailResponse> emails(HttpServletRequest request) { return emails.list(user(request)); }
    @GetMapping("/emails/{id}/content") public EmailContentResponse content(@PathVariable long id, HttpServletRequest request) { return emails.content(user(request), id); }
    @GetMapping("/email-feed") public List<RankedEmailFeedItemResponse> feed(HttpServletRequest request) { return feeds.feedForUser(user(request)); }
    @PostMapping("/gmail/sync") public ResponseEntity<RetrievalSummary> sync(@RequestHeader(value="X-CampusSignal-CSRF", required=false) String csrf, HttpServletRequest request) throws Exception { return ResponseEntity.ok(gmail.sync(mutate(request, csrf))); }
    @GetMapping("/actions") public List<edu.campussignal.dto.EmailActionReadResponse> actions(HttpServletRequest request) { return actions.list(user(request)); }
    @PostMapping("/emails/{id}/actions") public ResponseEntity<EmailActionResponse> createAction(@PathVariable long id, @RequestHeader(value="X-CampusSignal-CSRF", required=false) String csrf, HttpServletRequest request, @Valid @RequestBody EmailActionCreateRequest body) { return ResponseEntity.status(HttpStatus.CREATED).body(actions.create(mutate(request, csrf), id, body)); }
    @PatchMapping("/actions/{id}/status") public EmailActionResponse actionStatus(@PathVariable long id, @RequestHeader(value="X-CampusSignal-CSRF", required=false) String csrf, HttpServletRequest request, @Valid @RequestBody EmailActionStatusRequest body) { return actions.updateStatus(mutate(request, csrf), id, body); }
    @GetMapping("/emails/{id}/action-candidate") public ResponseEntity<?> candidate(@PathVariable long id, HttpServletRequest request) { UserAccount user = user(request); emailRepository.findByIdAndUserId(id, user.getId()).orElseThrow(edu.campussignal.service.EmailNotFoundException::new); return candidates.candidate(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build()); }
    private UserAccount user(HttpServletRequest request) { return auth.current(request); }
    private UserAccount mutate(HttpServletRequest request, String csrf) { return auth.currentForMutation(request, csrf); }
}
