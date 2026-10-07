package edu.campussignal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.repository.EmailRepository;

@Service
public class EmailClassificationBackfillService {
    private final EmailRepository repository;
    private final EmailClassificationService classifier;

    public record Result(int updated) {}

    public EmailClassificationBackfillService(
            EmailRepository repository,
            EmailClassificationService classifier) {
        this.repository = repository;
        this.classifier = classifier;
    }

    @Transactional
    public Result backfill() {
        var incomplete = repository
                .findAllByCategoryIsNullOrClassificationConfidenceIsNullOrderByIdAsc();
        for (var email : incomplete) {
            var classification = classifier.classify(email.getSubject(), email.getBody());
            email.applyClassification(classification);
        }
        repository.flush();
        return new Result(incomplete.size());
    }
}
