package be.appify.prefab.core.pubsub;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PubSubUtilTest {
    private static final PubSubAdmin UNUSED_PUB_SUB_ADMIN = null;
    private static final PubSubSubscriberTemplate UNUSED_SUBSCRIBER_TEMPLATE = null;
    private static final PubSubDeserializer UNUSED_DESERIALIZER = null;

    @Test
    void tryTopicForTypeReturnsEmptyWhenNoTopicMatches() {
        var pubSubUtil = pubSubUtil();

        assertEquals(Optional.empty(), pubSubUtil.tryTopicForType(UnmappedEvent.class));
    }

    @Test
    void topicForTypePrefersMostSpecificAssignableType() {
        var pubSubUtil = pubSubUtil();
        pubSubUtil.registerEventTopic("parent-topic", ParentEvent.class);
        pubSubUtil.registerEventTopic("base-topic", BaseEvent.class);

        assertEquals("parent-topic", pubSubUtil.topicForType(ChildEvent.class));
    }

    @Test
    void topicForTypeThrowsOnAmbiguousAssignableTypes() {
        var pubSubUtil = pubSubUtil();
        pubSubUtil.registerEventTopic("left-topic", LeftEvent.class);
        pubSubUtil.registerEventTopic("right-topic", RightEvent.class);

        assertThrows(IllegalStateException.class, () -> pubSubUtil.topicForType(AmbiguousEvent.class));
    }

    @Test
    void keyForThrowsOnAmbiguousAssignableExtractors() {
        var pubSubUtil = pubSubUtil();
        pubSubUtil.registerEventTopic("left-topic", LeftEvent.class, ignored -> "left");
        pubSubUtil.registerEventTopic("right-topic", RightEvent.class, ignored -> "right");

        assertThrows(IllegalStateException.class, () -> pubSubUtil.keyFor(new AmbiguousEvent()));
    }

    private static PubSubUtil pubSubUtil() {
        return new PubSubUtil(
                "project-id",
                "app",
                "",
                5,
                1000,
                30000,
                1.5f,
                UNUSED_PUB_SUB_ADMIN,
                UNUSED_SUBSCRIBER_TEMPLATE,
                UNUSED_DESERIALIZER
        );
    }

    private interface BaseEvent {}

    private interface ParentEvent extends BaseEvent {}

    private static final class ChildEvent implements ParentEvent {}

    private interface LeftEvent {}

    private interface RightEvent {}

    private static final class AmbiguousEvent implements LeftEvent, RightEvent {}

    private static final class UnmappedEvent {}
}
