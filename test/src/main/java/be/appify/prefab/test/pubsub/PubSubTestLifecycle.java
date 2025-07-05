package be.appify.prefab.test.pubsub;

import be.appify.prefab.processor.pubsub.PubSubUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class PubSubTestLifecycle {
    private final PubSubUtil pubSubUtil;

    public PubSubTestLifecycle(PubSubUtil pubSubUtil) {
        this.pubSubUtil = pubSubUtil;
    }

    @PreDestroy
    public void cleanUp() {
        pubSubUtil.deleteAllSubscriptions();
        pubSubUtil.deleteAllTopics();
    }
}
