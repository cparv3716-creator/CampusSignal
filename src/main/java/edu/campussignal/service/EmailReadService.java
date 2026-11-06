package edu.campussignal.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.dto.EmailContentResponse;
import edu.campussignal.dto.EmailResponse;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.entity.UserAccount;

@Service
@Transactional(readOnly = true)
public class EmailReadService {
    static final int MAX_CONTENT_PREVIEW_LENGTH = 12_000;
    private final EmailRepository repository;

    public EmailReadService(EmailRepository repository) {
        this.repository = repository;
    }

    public List<EmailResponse> list() {
        return repository.findAllByOrderByReceivedAtDescIdDesc().stream()
                .map(this::response)
                .toList();
    }

    public EmailResponse get(long id) {
        return response(repository.findById(id).orElseThrow(EmailNotFoundException::new));
    }

    public EmailContentResponse content(long id) {
        Email email = repository.findById(id).orElseThrow(EmailNotFoundException::new);
        String body = email.getBody();
        boolean truncated = body.length() > MAX_CONTENT_PREVIEW_LENGTH;
        String preview = truncated ? body.substring(0, MAX_CONTENT_PREVIEW_LENGTH) : body;
        return new EmailContentResponse(
                email.getId(),
                email.getSubject(),
                email.getSender(),
                email.getReceivedAt(),
                preview,
                truncated);
    }
    public List<EmailResponse> list(UserAccount user) { return repository.findAllByUserIdOrderByReceivedAtDescIdDesc(user.getId()).stream().map(this::response).toList(); }
    public EmailContentResponse content(UserAccount user, long id) {
        Email email = repository.findByIdAndUserId(id, user.getId()).orElseThrow(EmailNotFoundException::new);
        String body = email.getBody(); boolean truncated = body.length() > MAX_CONTENT_PREVIEW_LENGTH;
        return new EmailContentResponse(email.getId(), email.getSubject(), email.getSender(), email.getReceivedAt(), truncated ? body.substring(0, MAX_CONTENT_PREVIEW_LENGTH) : body, truncated);
    }

    private EmailResponse response(Email email) {
        var category = email.getCategory();
        var confidence = email.getClassificationConfidence();
        if (category == null || confidence == null) {
            throw new IncompleteEmailClassificationException();
        }
        return new EmailResponse(
                email.getId(),
                email.getSubject(),
                email.getSender(),
                email.getReceivedAt(),
                category,
                confidence.persistedValue());
    }
}
