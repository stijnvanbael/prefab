package be.appify.prefab.processor.pubsub;

import be.appify.prefab.processor.spring.JsonUtil;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PubSubUtil {
    private final String projectId;
    private final PubSubAdmin pubSubAdmin;
    private final PubSubSubscriberTemplate subscriberTemplate;
    private final JsonUtil jsonUtil;

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
        subscriberTemplate.subscribe(subscriptionName, message -> {
            try {
                var payload = message.getPubsubMessage().getData().toStringUtf8();
                consumer.accept(jsonUtil.parseJson(payload, type));
                message.ack();
            } catch (Exception e) {
                message.nack();
                throw e;
            }
        });
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
