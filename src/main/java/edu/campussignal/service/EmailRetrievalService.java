package edu.campussignal.service;

import java.io.IOException;
import org.springframework.stereotype.Service;
import edu.campussignal.dto.RetrievalSummary;

@Service
public class EmailRetrievalService {
    private final EmailIngestionService ingestion;

    public EmailRetrievalService(EmailIngestionService ingestion) { this.ingestion = ingestion; }

    public RetrievalSummary retrieve(EmailSource source, int maxResults) throws IOException {
        if (maxResults < 1 || maxResults > 500) {
            throw new IllegalArgumentException("maxResults must be between 1 and 500");
        }
        var ids = source.listMessageIds(maxResults);
        int created = 0;
        int duplicates = 0;
        int failed = 0;
        for (String id : ids) {
            try {
                if (ingestion.ingest(source.fetch(id)).created()) {
                    created++;
                } else {
                    duplicates++;
                }
            } catch (IOException | RuntimeException exception) {
                // Do not log exception payloads: provider/SQL errors can contain private email data.
                failed++;
            }
        }
        return new RetrievalSummary(ids.size(), created, duplicates, failed);
    }
}
