package edu.campussignal.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.security.LocalOperatorSessionService;
import edu.campussignal.security.LocalRequestSecurity;
import edu.campussignal.service.GmailSyncService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GmailSyncControllerTest {
    private static final String SECRET = "a".repeat(32);

    @Test
    void bootstrapRejectsWrongSecretAndIssuesSingleUseCodeForLoopback() {
        var sessions = new LocalOperatorSessionService(SECRET);
        var controller = new GmailSyncController(
                sessions, new LocalRequestSecurity("http://127.0.0.1:5174", "127.0.0.1"),
                mock(GmailSyncService.class));
        var request = loopbackRequest();

        assertThat(controller.bootstrap("wrong", request).getStatusCode().value()).isEqualTo(403);
        var response = controller.bootstrap(SECRET, request);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isInstanceOf(String.class);
    }

    @Test
    void bootstrapRejectsCrossSiteOrigin() {
        var controller = new GmailSyncController(
                new LocalOperatorSessionService(SECRET),
                new LocalRequestSecurity("http://127.0.0.1:5174", "127.0.0.1"),
                mock(GmailSyncService.class));
        var request = loopbackRequest();
        request.addHeader("Origin", "http://evil.example");
        assertThat(controller.bootstrap(SECRET, request).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void syncRequiresSessionAndCsrfAndReturnsCounts() throws Exception {
        var sessions = new LocalOperatorSessionService(SECRET);
        var sync = mock(GmailSyncService.class);
        when(sync.sync()).thenReturn(new RetrievalSummary(2, 1, 1, 0));
        var controller = new GmailSyncController(
                sessions, new LocalRequestSecurity("http://127.0.0.1:5174", "127.0.0.1"), sync);
        var request = loopbackRequest();
        assertThat(controller.sync("missing", request).getStatusCode().value()).isEqualTo(403);

        String code = (String) controller.bootstrap(SECRET, request).getBody();
        var sessionResponse = new MockHttpServletResponse();
        var grantResponse = controller.exchange(
                new edu.campussignal.dto.GmailSyncRequest(code), request, sessionResponse);
        assertThat(grantResponse.getStatusCode().is2xxSuccessful()).isTrue();
        String cookieValue = sessionResponse.getHeader("Set-Cookie").split(";")[0].split("=", 2)[1];
        request.setCookies(new Cookie("CAMPUSSIGNAL_OPERATOR_SESSION", cookieValue));
        String csrf = ((edu.campussignal.dto.GmailSessionResponse) grantResponse.getBody()).csrfToken();

        var result = controller.sync(csrf, request);
        assertThat(result.getBody()).isEqualTo(new RetrievalSummary(2, 1, 1, 0));
        verify(sync).sync();
    }

    @Test
    void restoresExistingCookieWithFreshCsrfWithoutOperatorSecret() {
        var sessions = new LocalOperatorSessionService(SECRET);
        var controller = new GmailSyncController(
                sessions, new LocalRequestSecurity("http://127.0.0.1:5174", "127.0.0.1"),
                mock(GmailSyncService.class));
        var request = loopbackRequest();
        String code = (String) controller.bootstrap(SECRET, request).getBody();
        var exchangeResponse = new MockHttpServletResponse();
        controller.exchange(new edu.campussignal.dto.GmailSyncRequest(code), request, exchangeResponse);
        String cookieValue = exchangeResponse.getHeader("Set-Cookie").split(";")[0].split("=", 2)[1];
        request.setCookies(new Cookie("CAMPUSSIGNAL_OPERATOR_SESSION", cookieValue));

        var restored = controller.restore(request);

        assertThat(restored.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(restored.getBody()).isInstanceOf(edu.campussignal.dto.GmailSessionResponse.class);
        assertThat(((edu.campussignal.dto.GmailSessionResponse) restored.getBody()).csrfToken()).isNotBlank();
    }

    @Test
    void restoreRejectsMissingOrCrossSiteSession() {
        var controller = new GmailSyncController(
                new LocalOperatorSessionService(SECRET),
                new LocalRequestSecurity("http://127.0.0.1:5174", "127.0.0.1"),
                mock(GmailSyncService.class));
        var request = loopbackRequest();
        assertThat(controller.restore(request).getStatusCode().value()).isEqualTo(401);
        request.addHeader("Origin", "http://evil.example");
        assertThat(controller.restore(request).getStatusCode().value()).isEqualTo(403);
    }

    private MockHttpServletRequest loopbackRequest() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.setLocalAddr("127.0.0.1");
        request.setServerName("127.0.0.1");
        return request;
    }
}
