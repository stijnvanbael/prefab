package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * Lifecycle management for Pub/Sub tests.
 */
@Component
@ConditionalOnBean(PubSubUtil.class)
public class PubSubTestLifecycle {
    private final PubSubUtil pubSubUtil;

    /**
     * Constructs a new PubSubTestLifecycle.
     *
     * @param pubSubUtil the Pub/Sub utility
     */
    public PubSubTestLifecycle(PubSubUtil pubSubUtil) {
        this.pubSubUtil = pubSubUtil;
    }

    /**
     * Cleans up all Pub/Sub topics and subscriptions when the context is destroyed.
     */
    @PreDestroy
    public void cleanUp() {
        pubSubUtil.deleteAllSubscriptions();
        pubSubUtil.deleteAllTopics();
    }
}
