package sns.createorupdate.infrastructure.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sns.createorupdate.MessageEvent;
import sns.createorupdate.application.ChannelSummaryService;

@Component
public class ChannelSummarySqsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(ChannelSummarySqsSubscriber.class);

    private final Executor executor;

    private final ChannelSummaryService channelSummaryService;

    public ChannelSummarySqsSubscriber(ChannelSummaryService channelSummaryService, SqsUtil sqsUtil,
            @Value("${topic.message.name}") String messageEventTopic) {
        executor = Executors.newFixedThreadPool(1);
        sqsUtil.subscribe(new SqsSubscriptionRequest<MessageEvent>(messageEventTopic, "channel-summary-on-message-event", MessageEvent.class, this::onMessageEvent)
                .withExecutor(executor));
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

