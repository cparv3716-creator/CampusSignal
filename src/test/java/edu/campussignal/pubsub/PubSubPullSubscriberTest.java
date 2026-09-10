package edu.campussignal.pubsub;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class PubSubPullSubscriberTest {
    private final PubSubPullSubscriber subscriber = new PubSubPullSubscriber();

    @Test
    void acknowledgesOnlyAfterSuccessfulProcessing() throws Exception {
        var consumer = mock(AckReplyConsumer.class);
        var message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("payload")).build();
        var handler = mock(PubSubPullSubscriber.Handler.class);
        subscriber.dispatch(message, consumer, handler);
        verify(handler).handle("payload".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        verify(consumer).ack();
        verify(consumer, never()).nack();
    }

    @Test
    void nacksFailedProcessing() throws Exception {
        var consumer = mock(AckReplyConsumer.class);
        var message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("private")).build();
        var handler = mock(PubSubPullSubscriber.Handler.class);
        doThrow(new IllegalStateException("private detail")).when(handler).handle(any());
        subscriber.dispatch(message, consumer, handler);
        verify(consumer).nack();
        verify(consumer, never()).ack();
    }
}
