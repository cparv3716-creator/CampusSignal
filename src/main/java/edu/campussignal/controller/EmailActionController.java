package edu.campussignal.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionReadResponse;
import edu.campussignal.dto.EmailActionResponse;
import edu.campussignal.dto.EmailActionStatusRequest;
import edu.campussignal.service.EmailActionService;

@RestController
@RequestMapping("/api/v1")
public class EmailActionController {
    private final EmailActionService actions;

    public EmailActionController(EmailActionService actions) {
        this.actions = actions;
    }

    @PostMapping("/emails/{emailId}/actions")
    public ResponseEntity<EmailActionResponse> create(
            @PathVariable long emailId,
            @Valid @RequestBody EmailActionCreateRequest request) {
        var action = actions.create(emailId, request);
        return ResponseEntity.created(URI.create("/api/v1/actions/" + action.id())).body(action);
    }

    @GetMapping("/actions")
    public List<EmailActionReadResponse> list() {
        return actions.list();
    }

    @GetMapping("/actions/{actionId}")
    public EmailActionReadResponse get(@PathVariable long actionId) {
        return actions.get(actionId);
    }

    @GetMapping("/emails/{emailId}/actions")
    public List<EmailActionReadResponse> listForEmail(@PathVariable long emailId) {
        return actions.listForEmail(emailId);
    }

    @PatchMapping("/actions/{actionId}/status")
    public EmailActionResponse updateStatus(
            @PathVariable long actionId,
            @Valid @RequestBody EmailActionStatusRequest request) {
        return actions.updateStatus(actionId, request);
    }
}
