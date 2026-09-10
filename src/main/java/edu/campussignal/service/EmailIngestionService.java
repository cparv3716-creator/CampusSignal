package edu.campussignal.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import edu.campussignal.dto.IncomingEmail;
import edu.campussignal.entity.Email;
import edu.campussignal.repository.EmailRepository;

@Service
public class EmailIngestionService {
    private final EmailRepository repository;
    private final TransactionTemplate transaction;

    public record Result(long id, boolean created) {}

    public EmailIngestionService(EmailRepository repository, PlatformTransactionManager manager) {
        this.repository = repository;
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
                var saved = repository.saveAndFlush(new Email(incoming));
                return new Result(saved.getId(), true);
            });
        } catch (DataIntegrityViolationException exception) {
            // The failed insert has rolled back before checking a concurrent winner.
            return repository.findByGmailMessageId(incoming.messageId())
                    .map(email -> new Result(email.getId(), false))
                    .orElseThrow(() -> exception);
        }
    }
}
