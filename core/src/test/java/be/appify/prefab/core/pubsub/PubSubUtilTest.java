package be.appify.prefab.core.pubsub;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class PubSubUtilTest {

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
                mock(com.google.cloud.spring.pubsub.PubSubAdmin.class),
                mock(com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate.class),
                mock(PubSubDeserializer.class)
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
