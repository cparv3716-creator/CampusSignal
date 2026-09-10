package edu.campussignal.pubsub;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PubSubPullSubscriber {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PubSubPullSubscriber.class);

    @FunctionalInterface
    public interface Handler {
        void handle(byte[] data) throws Exception;
    }

    public void listen(String subscriptionPath, Handler handler) {
        var subscription = ProjectSubscriptionName.parse(subscriptionPath);
        MessageReceiver receiver = (message, consumer) -> dispatch(message, consumer, handler);
        var flowControl = FlowControlSettings.newBuilder()
                .setMaxOutstandingElementCount(1L)
                .setMaxOutstandingRequestBytes(10L * 1024L * 1024L)
                .build();
        var subscriber = Subscriber.newBuilder(subscription, receiver)
                .setFlowControlSettings(flowControl).build();
        try {
            subscriber.startAsync().awaitRunning();
            subscriber.awaitTerminated();
        } finally {
            subscriber.stopAsync();
        }
    }

    void dispatch(PubsubMessage message, AckReplyConsumer consumer, Handler handler) {
        try {
            handler.handle(message.getData().toByteArray());
            consumer.ack();
        } catch (Exception exception) {
            // Never log Pub/Sub payloads or provider exception messages: they can contain private data.
            LOGGER.error("Gmail notification processing failed: {}", exception.getClass().getSimpleName());
            consumer.nack();
        }
    }
}
