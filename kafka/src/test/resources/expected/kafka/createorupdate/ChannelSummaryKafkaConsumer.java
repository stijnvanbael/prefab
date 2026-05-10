package kafka.createorupdate.infrastructure.kafka;

import kafka.createorupdate.MessageEvent;
import kafka.createorupdate.application.ChannelSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ChannelSummaryKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(ChannelSummaryKafkaConsumer.class);

    private final ChannelSummaryService channelSummaryService;

    public ChannelSummaryKafkaConsumer(ChannelSummaryService channelSummaryService) {
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
