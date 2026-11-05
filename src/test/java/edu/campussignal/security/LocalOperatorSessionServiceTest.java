package edu.campussignal.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LocalOperatorSessionServiceTest {
    private static final String SECRET = "a".repeat(32);

    @Test
    void requiresStrongSecretAndComparesItWithoutExposingIt() {
        var service = new LocalOperatorSessionService(SECRET);
        assertThat(service.isConfigured()).isTrue();
        assertThat(service.matchesSecret(SECRET)).isTrue();
        assertThat(service.matchesSecret("wrong")).isFalse();
    }

    @Test
    void bootstrapCodesAreSingleUseAndSessionsRequireCsrf() {
        var service = new LocalOperatorSessionService(SECRET);
        String code = service.issueBootstrapCode();
        var grant = service.exchange(code);
        assertThat(grant).isNotNull();
        assertThat(service.exchange(code)).isNull();
        assertThat(service.valid(grant.sessionId(), grant.csrfToken())).isTrue();
        assertThat(service.valid(grant.sessionId(), "wrong")).isFalse();
    }

    @Test
    void rejectsShortOperatorSecret() {
        assertThat(new LocalOperatorSessionService("short").isConfigured()).isFalse();
    }
}
