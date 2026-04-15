package kafka.createorupdate.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.createorupdate.MessageEvent;
import kafka.createorupdate.application.ChannelSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ChannelSummaryKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(ChannelSummaryKafkaConsumer.class);

    private final ChannelSummaryService channelSummaryService;

    public ChannelSummaryKafkaConsumer(ChannelSummaryService channelSummaryService,
            KafkaJsonTypeResolver typeResolver,
            @Value("${topic.message.name}") String messageEventTopic) {
        typeResolver.registerType(messageEventTopic, MessageEvent.class);
        this.channelSummaryService = channelSummaryService;
    }

    @KafkaListener(
            topics = "${topic.message.name}",
            groupId = "${spring.application.name}.channel-summary-on-message-event",
            concurrency = "1"
    )
    public void onMessageEvent(MessageEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case MessageEvent.Sent e -> channelSummaryService.onUpdate(e);
            default -> {
            }
        }
    }
}
