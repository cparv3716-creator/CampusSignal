package edu.campussignal.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import edu.campussignal.dto.EmailActionCreateRequest;
import edu.campussignal.dto.EmailActionReadResponse;
import edu.campussignal.dto.EmailActionResponse;
import edu.campussignal.dto.EmailActionStatus;
import edu.campussignal.dto.EmailActionStatusRequest;
import edu.campussignal.entity.EmailAction;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;

@Service
@Validated
@Transactional(readOnly = true)
public class EmailActionService {
    private final EmailActionRepository actions;
    private final EmailRepository emails;
    private final Clock clock;
    private final ActionDeadlinePresentationService deadlinePresentation;

    public EmailActionService(
            EmailActionRepository actions,
            EmailRepository emails,
            Clock clock,
            ActionDeadlinePresentationService deadlinePresentation) {
        this.actions = actions;
        this.emails = emails;
        this.clock = clock;
        this.deadlinePresentation = deadlinePresentation;
    }

    @Transactional
    public EmailActionResponse create(long emailId, @Valid EmailActionCreateRequest request) {
        var email = emails.findById(emailId).orElseThrow(EmailNotFoundException::new);
        return response(actions.saveAndFlush(EmailAction.pending(email, request)));
    }

    public List<EmailActionReadResponse> list() {
        return actions.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(this::readResponse)
                .toList();
    }

    public List<EmailActionReadResponse> listForEmail(long emailId) {
        if (!emails.existsById(emailId)) {
            throw new EmailNotFoundException();
        }
        return actions.findAllByEmailIdOrderByCreatedAtDescIdDesc(emailId).stream()
                .map(this::readResponse)
                .toList();
    }

    public EmailActionReadResponse get(long actionId) {
        return readResponse(findAction(actionId));
    }

    @Transactional
    public EmailActionResponse updateStatus(
            long actionId,
            @Valid EmailActionStatusRequest request) {
        var action = findAction(actionId);
        transition(action, request.status());
        return response(action);
    }

    private EmailAction findAction(long actionId) {
        return actions.findById(actionId).orElseThrow(EmailActionNotFoundException::new);
    }

    private void transition(EmailAction action, EmailActionStatus requested) {
        EmailActionStatus current = action.getStatus();
        if (current == requested && current != EmailActionStatus.PENDING) {
            return;
        }
        if (current != EmailActionStatus.PENDING) {
            throw new InvalidEmailActionTransitionException();
        }
        switch (requested) {
            case COMPLETED -> action.markCompleted(Instant.now(clock));
            case NOT_INTERESTED -> action.markNotInterested();
            case PENDING -> throw new InvalidEmailActionTransitionException();
        }
    }

    private EmailActionResponse response(EmailAction action) {
        return new EmailActionResponse(
                action.getId(),
                action.getEmail().getId(),
                action.getEmail().getSubject(),
                action.getActionType(),
                action.getStatus(),
                action.getDeadline(),
                action.getActionUrl(),
                action.getCompletedAt(),
                action.getCreatedAt());
    }

    private EmailActionReadResponse readResponse(EmailAction action) {
        var presentation = deadlinePresentation.present(action.getStatus(), action.getDeadline());
        return new EmailActionReadResponse(
                action.getId(),
                action.getEmail().getId(),
                action.getEmail().getSubject(),
                action.getActionType(),
                action.getStatus(),
                presentation.effectiveStatus(),
                presentation.urgency(),
                presentation.daysRemaining(),
                action.getDeadline(),
                action.getActionUrl(),
                action.getCompletedAt(),
                action.getCreatedAt());
    }
}
