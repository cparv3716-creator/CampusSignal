package edu.campussignal.service;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.campussignal.entity.GmailWatchState;
import edu.campussignal.repository.GmailWatchStateRepository;

@Service
public class GmailWatchStateService {
    public record Snapshot(String lastHistoryId, String watchHistoryId, Instant watchExpiresAt) {}

    private final GmailWatchStateRepository repository;

    public GmailWatchStateService(GmailWatchStateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Snapshot> get() {
        return repository.findById(GmailWatchState.SINGLETON_ID).map(GmailWatchStateService::snapshot);
    }

    @Transactional
    public Snapshot recordWatch(String historyId, Instant expiresAt) {
        var state = repository.findById(GmailWatchState.SINGLETON_ID).orElse(null);
        if (state == null) {
            state = new GmailWatchState(historyId, expiresAt);
        } else {
            state.renew(historyId, expiresAt);
        }
        return snapshot(repository.saveAndFlush(state));
    }

    @Transactional
    public Snapshot advance(String historyId) {
        var state = repository.findById(GmailWatchState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Gmail watch state has not been initialized"));
        state.advance(historyId);
        return snapshot(repository.saveAndFlush(state));
    }

    private static Snapshot snapshot(GmailWatchState state) {
        return new Snapshot(state.getLastHistoryId(), state.getWatchHistoryId(), state.getWatchExpiresAt());
    }
}
