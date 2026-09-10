package edu.campussignal.gmail;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import edu.campussignal.config.GmailProperties;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.service.EmailRetrievalService;
import edu.campussignal.service.GmailListenerService;
import edu.campussignal.service.GmailWatchService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class GmailCommandTest {
    private final GmailAuthorizationService authorization = mock(GmailAuthorizationService.class);
    private final EmailRetrievalService retrieval = mock(EmailRetrievalService.class);
    private final GmailWatchService watch = mock(GmailWatchService.class);
    private final GmailListenerService listener = mock(GmailListenerService.class);
    private final GmailProperties settings = new GmailProperties(
            Path.of("synthetic-client.json"), Path.of("unused-token-directory"), 8888, 10,
            "me", "project", "mail-topic", "mail-sub", 10);

    private GmailCommand command(String name) {
        return new GmailCommand(name, settings, authorization, retrieval, watch, listener);
    }

    @Test
    void authorizationIsExplicitAndDoesNotRetrieve() throws Exception {
        var command = command("authorize");
        command.run(new DefaultApplicationArguments());
        verify(authorization).authorize();
        verifyNoInteractions(retrieval, watch, listener);
        assertThat(command.getExitCode()).isZero();
    }

    @Test
    void retrievalPrintsCountsAndReportsPartialFailure(CapturedOutput output) throws Exception {
        var source = mock(GmailApiClient.class);
        when(authorization.client()).thenReturn(source);
        when(retrieval.retrieve(source, 10)).thenReturn(new RetrievalSummary(3, 1, 1, 1));
        var command = command("retrieve");
        command.run(new DefaultApplicationArguments());
        assertThat(command.getExitCode()).isEqualTo(1);
        assertThat(output.getOut()).contains("fetched=3 created=1 alreadyExisted=1 failed=1");
    }

    @Test
    void watchRegistersConfiguredTopic(CapturedOutput output) throws Exception {
        var source = mock(GmailApiClient.class);
        when(authorization.client()).thenReturn(source);
        when(watch.register(source, "projects/project/topics/mail-topic"))
                .thenReturn(new GmailWatchService.Registration("123",
                        Instant.parse("2026-09-17T08:00:00Z"), "123"));
        var command = command("watch");
        command.run(new DefaultApplicationArguments());
        assertThat(command.getExitCode()).isZero();
        verify(watch).register(source, "projects/project/topics/mail-topic");
        assertThat(output.getOut()).contains("Watch registered", "History ID: 123");
    }

    @Test
    void listenerUsesConfiguredSubscription() throws Exception {
        var source = mock(GmailApiClient.class);
        when(authorization.client()).thenReturn(source);
        var command = command("listen");
        command.run(new DefaultApplicationArguments());
        verify(listener).listen(source, "projects/project/subscriptions/mail-sub", 10);
        assertThat(command.getExitCode()).isZero();
    }

    @Test
    void errorsAreReportedWithoutProviderDetails(CapturedOutput output) throws Exception {
        when(authorization.client()).thenThrow(new IOException("synthetic-private-detail"));
        var command = command("retrieve");
        command.run(new DefaultApplicationArguments());
        assertThat(command.getExitCode()).isEqualTo(1);
        assertThat(output.getErr()).contains("Gmail command failed").doesNotContain("synthetic-private-detail");
    }

    @Test
    void invalidCommandFailsWithoutCallingGoogle() {
        var command = command("unsupported");
        command.run(new DefaultApplicationArguments());
        assertThat(command.getExitCode()).isEqualTo(1);
        verifyNoInteractions(authorization, retrieval, watch, listener);
    }
}
