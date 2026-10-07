package edu.campussignal.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import edu.campussignal.service.EmailClassificationBackfillService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class EmailClassificationBackfillCommandTest {
    @Test
    void explicitCommandRunsBackfillAndReportsUpdatedCount(CapturedOutput output) throws Exception {
        var service = mock(EmailClassificationBackfillService.class);
        when(service.backfill()).thenReturn(new EmailClassificationBackfillService.Result(3));
        var command = new EmailClassificationBackfillCommand(service);

        command.run(new DefaultApplicationArguments());

        verify(service).backfill();
        assertThat(output.getOut()).contains("classificationBackfillUpdated=3");
    }
}
