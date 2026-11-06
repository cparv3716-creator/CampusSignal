package edu.campussignal.service;
import edu.campussignal.entity.UserAccount;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.gmail.GmailAuthorizationService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class StudentGmailSyncService {
    private final GoogleWebOAuthService oauth; private final GmailAuthorizationService gmail; private final EmailRetrievalService retrieval; private final int maximum;
    private final java.util.Set<Long> active = ConcurrentHashMap.newKeySet();
    public StudentGmailSyncService(GoogleWebOAuthService oauth, GmailAuthorizationService gmail, EmailRetrievalService retrieval, @Value("${gmail.sync-max-results:10}") int maximum) { this.oauth = oauth; this.gmail = gmail; this.retrieval = retrieval; this.maximum = maximum; }
    public RetrievalSummary sync(UserAccount user) throws IOException, InterruptedException, GeneralSecurityException {
        if (!active.add(user.getId())) throw new SyncAlreadyRunningException();
        try { return retrieval.retrieve(gmail.clientForAccessToken(oauth.accessToken(user)), maximum, user); }
        finally { active.remove(user.getId()); }
    }
    public static class SyncAlreadyRunningException extends RuntimeException { private static final long serialVersionUID = 1L; }
}
