package edu.campussignal.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.gmail.GmailApiClient;
import edu.campussignal.gmail.GmailAuthorizationService;

@Service
public class GmailSyncService {
    private final GmailAuthorizationService authorization;
    private final EmailRetrievalService retrieval;
    private final int maximum;
    private final AtomicBoolean running = new AtomicBoolean();

    public GmailSyncService(GmailAuthorizationService authorization, EmailRetrievalService retrieval,
                            @Value("${gmail.sync-max-results:10}") int maximum) {
        if (maximum < 1 || maximum > 10) {
            throw new IllegalArgumentException("gmail.sync-max-results must be between 1 and 10");
        }
        this.authorization = authorization;
        this.retrieval = retrieval;
        this.maximum = maximum;
    }

    public RetrievalSummary sync() throws IOException, GeneralSecurityException {
        if (!running.compareAndSet(false, true)) {
            throw new SyncAlreadyRunningException();
        }
        try {
            GmailApiClient client = authorization.client();
            return retrieval.retrieve(client, maximum);
        } finally {
            running.set(false);
        }
    }

    public static class SyncAlreadyRunningException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
