package pubsub.single.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.spring.JsonUtil;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pubsub.single.UserCreated;

@Component
public class UserCreatedPubSubPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserCreatedPubSubPublisher.class);

    private final PubSubTemplate pubSubTemplate;

    private final JsonUtil jsonSupport;

    private final String topic;

    public UserCreatedPubSubPublisher(PubSubTemplate pubSubTemplate, PubSubUtil pubSub,
            JsonUtil jsonSupport, @Value("user") String topic) {
        this.pubSubTemplate = pubSubTemplate;
        this.jsonSupport = jsonSupport;
        this.topic = pubSub.ensureTopicExists(topic);
    }

    @EventListener
    public void publish(UserCreated event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        pubSubTemplate.publish(
                    topic,
                    PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8(jsonSupport.toJson(event)))
                        .setOrderingKey(event.id())
                        .putAttributes("type", event.getClass().getName())
                        .build());
    }
}
