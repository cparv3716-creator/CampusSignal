package edu.campussignal.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.dto.EmailResponse;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailRepository;

@Service
@Transactional(readOnly = true)
public class EmailReadService {
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
