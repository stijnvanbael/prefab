package pubsub.createorupdate.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.createorupdate.MessageEvent;
import pubsub.createorupdate.application.ChannelSummaryService;

@Component
public class ChannelSummaryPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(ChannelSummaryPubSubSubscriber.class);

    private final Executor messageEventExecutor;

    private final ChannelSummaryService channelSummaryService;

    public ChannelSummaryPubSubSubscriber(ChannelSummaryService channelSummaryService,
            PubSubUtil pubSub, @Value("${topic.message.name}") String messageEventTopic) {
        messageEventExecutor = Executors.newFixedThreadPool(1);
        pubSub.subscribe(new SubscriptionRequest<MessageEvent>(messageEventTopic, "channel-summary-on-message-event", MessageEvent.class, this::onMessageEvent)
                .withExecutor(messageEventExecutor));
        this.channelSummaryService = channelSummaryService;
    }

    private void onMessageEvent(MessageEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case MessageEvent.Sent e -> channelSummaryService.onUpdate(e);
            default -> {
            }
        }
    }
}
