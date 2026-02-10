package pubsub.single.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pubsub.single.UserCreated;

@Component
public class UserCreatedPubSubPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserCreatedPubSubPublisher.class);

    private final PubSubTemplate pubSubTemplate;

    private final PubSubSerializer serializer;

    private final String topic;

    public UserCreatedPubSubPublisher(PubSubTemplate pubSubTemplate, PubSubUtil pubSub,
            PubSubSerializer serializer) {
        this.pubSubTemplate = pubSubTemplate;
        this.serializer = serializer;
        this.topic = pubSub.ensureTopicExists("user");
    }

    @EventListener
    public void publish(UserCreated event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        pubSubTemplate.publish(
                    topic,
                    PubsubMessage.newBuilder()
                        .setData(ByteString.copyFrom(serializer.serialize(PubSubUtil.simpleTopicName(topic), event)))
                        .setOrderingKey(event.user().id())
                        .putAttributes("type", event.getClass().getName())
                        .build());
    }
}
