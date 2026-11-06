package edu.campussignal.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.entity.UserAccount;

@Service
public class EmailIngestionService {
    private final EmailRepository repository;
    private final EmailClassificationService classifier;
    private final TransactionTemplate transaction;

    public record Result(long id, boolean created) {}

    public EmailIngestionService(EmailRepository repository, EmailClassificationService classifier,
                                 PlatformTransactionManager manager) {
        this.repository = repository;
        this.classifier = classifier;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Result ingest(IncomingEmail incoming) {
        var existing = repository.findByGmailMessageId(incoming.messageId());
        if (existing.isPresent()) {
            return new Result(existing.get().getId(), false);
        }
        try {
            return transaction.execute(status -> {
                var classification = classifier.classify(incoming.subject(), incoming.body());
                var saved = repository.saveAndFlush(Email.classified(incoming, classification));
                return new Result(saved.getId(), true);
            });
        } catch (DataIntegrityViolationException exception) {
            // The failed insert has rolled back before checking a concurrent winner.
            return repository.findByGmailMessageId(incoming.messageId())
                    .map(email -> new Result(email.getId(), false))
                    .orElseThrow(() -> exception);
        }
    }

    public Result ingest(UserAccount user, IncomingEmail incoming) {
        IncomingEmail owned = owned(user, incoming);
        var existing = repository.findByUserIdAndGmailMessageId(user.getId(), owned.messageId());
        if (existing.isPresent()) return new Result(existing.get().getId(), false);
        try {
            return transaction.execute(status -> {
                var saved = repository.saveAndFlush(Email.classified(user, owned, classifier.classify(owned.subject(), owned.body())));
                return new Result(saved.getId(), true);
            });
        } catch (DataIntegrityViolationException exception) {
            return repository.findByUserIdAndGmailMessageId(user.getId(), owned.messageId())
                    .map(email -> new Result(email.getId(), false)).orElseThrow(() -> exception);
        }
    }
    private static IncomingEmail owned(UserAccount user, IncomingEmail incoming) {
        // The historical table has a global unique provider-id constraint. Namespace new rows so
        // identical opaque Gmail IDs from separate accounts remain isolated without changing data.
        return new IncomingEmail("user-" + user.getId() + ":" + incoming.messageId(), incoming.subject(),
                incoming.sender(), incoming.receivedAt(), incoming.body());
    }
}
