package edu.campussignal.command;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import edu.campussignal.service.EmailClassificationBackfillService;

@Component
@ConditionalOnProperty(prefix = "classification", name = "backfill", havingValue = "true")
public class EmailClassificationBackfillCommand implements ApplicationRunner {
    private final EmailClassificationBackfillService backfill;

    public EmailClassificationBackfillCommand(EmailClassificationBackfillService backfill) {
        this.backfill = backfill;
    }

    @Override
    public void run(ApplicationArguments args) {
        var result = backfill.backfill();
        System.out.printf("classificationBackfillUpdated=%d%n", result.updated());
    }
}
