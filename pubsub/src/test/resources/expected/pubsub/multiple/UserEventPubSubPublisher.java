package pubsub.multiple.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pubsub.multiple.UserEvent;

@Component
public class UserEventPubSubPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserEventPubSubPublisher.class);

    private final PubSubTemplate pubSubTemplate;

    private final PubSubSerializer serializer;

    private final String topic;

    public UserEventPubSubPublisher(PubSubTemplate pubSubTemplate, PubSubUtil pubSub,
            PubSubSerializer serializer, @Value("${topic.user.name}") String topic) {
        this.pubSubTemplate = pubSubTemplate;
        this.serializer = serializer;
        this.topic = pubSub.ensureTopicExists(topic);
    }

    @EventListener
    public void publish(UserEvent event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        pubSubTemplate.publish(
                    topic,
                    PubsubMessage.newBuilder()
                        .setData(ByteString.copyFrom(serializer.serialize(PubSubUtil.simpleTopicName(topic), event)))
                        .setOrderingKey(event.id())
                        .putAttributes("type", event.getClass().getName())
                        .build());
    }
}
