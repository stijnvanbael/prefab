package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.spring.JsonUtil;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Component
@ConditionalOnClass(PubSubAdmin.class)
public class PubSubUtil {
    private static final Logger log = LoggerFactory.getLogger(PubSubUtil.class);
    private final String projectId;
    private final PubSubAdmin pubSubAdmin;
    private final PubSubSubscriberTemplate subscriberTemplate;
    private final JsonUtil jsonUtil;
    private final ConcurrentMap<String, Class<?>> messageTypes = new ConcurrentHashMap<>();

    public PubSubUtil(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            PubSubAdmin pubSubAdmin,
            PubSubSubscriberTemplate subscriberTemplate,
            JsonUtil jsonUtil
    ) {
        this.projectId = projectId;
        this.pubSubAdmin = pubSubAdmin;
        this.subscriberTemplate = subscriberTemplate;
        this.jsonUtil = jsonUtil;
    }

    public <T> void subscribe(String topic, String subscription, Class<T> type, Consumer<T> consumer) {
        var topicName = ensureTopicExists(topic);
        var subscriptionName = ensureSubscriptionExists(subscription, topicName);
        subscriberTemplate.subscribe(subscriptionName, message ->
                consume(type, consumer, message));
    }

    private <T> void consume(Class<T> type, Consumer<T> consumer, BasicAcknowledgeablePubsubMessage message) {
        try {
            var pubsubMessage = message.getPubsubMessage();
            if (pubsubMessage.containsAttributes("type")) {
                consumeTyped(type, consumer, pubsubMessage);
            } else {
                consumer.accept(jsonUtil.parseJson(pubsubMessage.getData().toStringUtf8(), type));
            }
        } catch (Exception e) {
            log.error("Error processing Pub/Sub message", e);
            throw e;
        } finally {
            message.ack(); // TODO: delivery semantics
        }
    }

    private <T> void consumeTyped(Class<T> type, Consumer<T> consumer, PubsubMessage pubsubMessage) {
        var typeName = pubsubMessage.getAttributesOrThrow("type");
        var consumedType = messageTypes.computeIfAbsent(typeName, key -> {
            try {
                return Class.forName(typeName);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Could not find class for type found in message: " + typeName, e);
            }
        });
        if (type.isAssignableFrom(consumedType)) {
            consumer.accept(jsonUtil.parseJson(pubsubMessage.getData().toStringUtf8(), type));
        }
    }

    public String ensureTopicExists(String topic) {
        var topicName = ProjectTopicName.of(projectId, topic).toString();
        if (pubSubAdmin.getTopic(topicName) == null) {
            pubSubAdmin.createTopic(topicName);
        }
        return topicName;
    }

    public void deleteAllSubscriptions() {
        pubSubAdmin.listSubscriptions().forEach(subscription -> {
            var subscriptionName = subscription.getName();
            pubSubAdmin.deleteSubscription(subscriptionName);
        });
    }

    public void deleteAllTopics() {
        pubSubAdmin.listTopics().forEach(topic -> {
            var topicName = topic.getName();
            pubSubAdmin.deleteTopic(topicName);
        });
    }

    private String ensureSubscriptionExists(String subscription, String fullyQualifiedTopic) {
        var subscriptionName = ProjectSubscriptionName.of(projectId, subscription).toString();
        if (pubSubAdmin.getSubscription(subscriptionName) == null) {
            pubSubAdmin.createSubscription(subscriptionName, fullyQualifiedTopic);
        }
        return subscriptionName;
    }

    public void deleteSubscription(String subscription) {
        var subscriptionName = ProjectSubscriptionName.of(projectId, subscription).toString();
        if (pubSubAdmin.getSubscription(subscriptionName) != null) {
            pubSubAdmin.deleteSubscription(subscriptionName);
        }
    }
}
