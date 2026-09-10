package edu.campussignal.gmail;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import edu.campussignal.config.GmailProperties;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.service.EmailRetrievalService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class GmailCommandTest {
    private final GmailAuthorizationService authorization = mock(GmailAuthorizationService.class);
    private final EmailRetrievalService retrieval = mock(EmailRetrievalService.class);
    private final GmailProperties settings = new GmailProperties(
            Path.of("synthetic-client.json"), Path.of("unused-token-directory"), 8888, 10);

    private GmailCommand command(String name) {
        return new GmailCommand(name, settings, authorization, retrieval);
    }

    @Test
    void authorizationIsExplicitAndDoesNotRetrieve() throws Exception {
        var command = command("authorize");
        command.run(new DefaultApplicationArguments());
        verify(authorization).authorize();
        verifyNoInteractions(retrieval);
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
        verifyNoInteractions(authorization, retrieval);
    }
}
