package edu.campussignal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.campussignal.dto.ActionCandidate;
import edu.campussignal.service.EmailActionCandidateService;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailActionCandidateController {
    private final EmailActionCandidateService candidates;

    public EmailActionCandidateController(EmailActionCandidateService candidates) {
        this.candidates = candidates;
    }

    @GetMapping("/{emailId}/action-candidate")
    public ResponseEntity<ActionCandidate> candidate(@PathVariable long emailId) {
        return candidates.candidate(emailId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
