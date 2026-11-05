package edu.campussignal.service;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.gmail.GmailApiClient;
import edu.campussignal.gmail.GmailAuthorizationService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GmailSyncServiceTest {
    private final GmailAuthorizationService authorization = mock(GmailAuthorizationService.class);
    private final EmailRetrievalService retrieval = mock(EmailRetrievalService.class);
    private final GmailApiClient client = mock(GmailApiClient.class);

    @Test
    void delegatesBoundedSyncAndReturnsCountsOnly() throws Exception {
        when(authorization.client()).thenReturn(client);
        when(retrieval.retrieve(client, 10)).thenReturn(new RetrievalSummary(10, 3, 6, 1));
        var result = new GmailSyncService(authorization, retrieval, 10).sync();
        assertThat(result).isEqualTo(new RetrievalSummary(10, 3, 6, 1));
        verify(retrieval).retrieve(client, 10);
    }

    @Test
    void rejectsConfiguredMaximumAboveTen() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GmailSyncService(authorization, retrieval, 11));
    }

    @Test
    void releasesConcurrencyGuardAfterAuthorizationFailure() throws Exception {
        when(authorization.client()).thenThrow(new IOException("synthetic authorization failure"))
                .thenReturn(client);
        when(retrieval.retrieve(client, 10)).thenReturn(new RetrievalSummary(0, 0, 0, 0));
        var service = new GmailSyncService(authorization, retrieval, 10);

        assertThatThrownBy(service::sync).isInstanceOf(IOException.class);
        assertThat(service.sync()).isEqualTo(new RetrievalSummary(0, 0, 0, 0));
    }

    @Test
    void rejectsOverlappingSyncWithoutStartingAnotherRetrieval() throws Exception {
        when(authorization.client()).thenReturn(client);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(retrieval.retrieve(client, 10)).thenAnswer(invocation -> {
            started.countDown();
            assertThat(release.await(2, TimeUnit.SECONDS)).isTrue();
            return new RetrievalSummary(1, 1, 0, 0);
        });
        var service = new GmailSyncService(authorization, retrieval, 10);
        var first = new Thread(() -> assertThatCode(service::sync).doesNotThrowAnyException());
        first.start();
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(service::sync)
                .isInstanceOf(GmailSyncService.SyncAlreadyRunningException.class);
        release.countDown();
        first.join(2_000);
        verify(retrieval, times(1)).retrieve(client, 10);
    }
}
