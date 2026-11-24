package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.kafka.EventRegistry;
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
        var eventRegistry = new EventRegistry();

        assertEquals(Optional.empty(), pubSubUtil(eventRegistry).tryTopicForType(UnmappedEvent.class));
    }

    @Test
    void topicForTypeResolvesPermittedSealedSubtype() {
        var eventRegistry = new EventRegistry();
        eventRegistry.registerType("parent-topic", ParentEvent.class);

        // ChildEvent is a permitted subtype of sealed ParentEvent — registered automatically
        assertEquals("parent-topic", pubSubUtil(eventRegistry).topicForType(ChildEvent.class));
    }

    @Test
    void topicForTypeThrowsWhenMultipleTopicsRegistered() {
        var eventRegistry = new EventRegistry();
        eventRegistry.registerType("topic1", AmbiguousEvent.class);
        eventRegistry.registerType("topic2", AmbiguousEvent.class);

        assertThrows(IllegalStateException.class, () -> pubSubUtil(eventRegistry).topicForType(AmbiguousEvent.class));
    }

    private static PubSubUtil pubSubUtil(EventRegistry eventRegistry) {
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
                UNUSED_DESERIALIZER,
                eventRegistry
        );
    }

    private sealed interface ParentEvent permits ChildEvent {}

    private static final class ChildEvent implements ParentEvent {}

    private static final class AmbiguousEvent {}

    private static final class UnmappedEvent {}
}
