package edu.campussignal.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.dto.ActionCandidate;
import edu.campussignal.repository.EmailRepository;

@Service
@Transactional(readOnly = true)
public class EmailActionCandidateService {
    private final EmailRepository emails;
    private final DeterministicEmailActionExtractor extractor;

    public EmailActionCandidateService(
            EmailRepository emails,
            DeterministicEmailActionExtractor extractor) {
        this.emails = emails;
        this.extractor = extractor;
    }

    public Optional<ActionCandidate> candidate(long emailId) {
        var email = emails.findById(emailId).orElseThrow(EmailNotFoundException::new);
        return extractor.extract(email.getSubject(), email.getBody());
    }
}
