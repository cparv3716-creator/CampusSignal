package edu.campussignal.service;

import org.springframework.stereotype.Service;
import edu.campussignal.gmail.GmailApiClient;
import edu.campussignal.gmail.GmailNotificationDecoder;
import edu.campussignal.pubsub.PubSubPullSubscriber;

@Service
public class GmailListenerService {
    private final PubSubPullSubscriber subscriber;
    private final GmailNotificationDecoder decoder;
    private final GmailHistorySyncService history;

    public GmailListenerService(PubSubPullSubscriber subscriber, GmailNotificationDecoder decoder,
                                GmailHistorySyncService history) {
        this.subscriber = subscriber;
        this.decoder = decoder;
        this.history = history;
    }

    public void listen(GmailApiClient client, String subscriptionPath, int recoveryMaxResults) {
        subscriber.listen(subscriptionPath, data -> {
            var notification = decoder.decode(data);
            var summary = history.sync(client, notification.historyId(), recoveryMaxResults);
            System.out.printf("History processed: messages=%d, created=%d, existing=%d, recovered=%s%n",
                    summary.messagesFound(), summary.created(), summary.alreadyExisted(),
                    summary.recoveredFromStaleHistory());
        });
    }
}
