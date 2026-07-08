package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void consumeTypedIgnoresMessageWithUnknownEventType() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("test-topic", KnownEvent.class, Event.Serialization.JSON);

        var received = new ArrayList<KnownEvent>();
        var subscriptionRequest = new SubscriptionRequest<>("test-topic", "sub", KnownEvent.class, received::add)
                .withExecutor(Runnable::run);

        var unknownTypeMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8("{}"))
                .putAttributes("type", "com.example.UnknownEvent")
                .build();

        pubSubUtil(eventRegistry).consumeTyped(subscriptionRequest, unknownTypeMessage);

        assertTrue(received.isEmpty(), "No events should be dispatched for unknown event types");
    }

    @Test
    void consumeTypedProcessesKnownEventTypeAfterUnknownEventType() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("test-topic", KnownEvent.class, Event.Serialization.JSON);

        var knownEvent = new KnownEvent("test");
        var deserializer = mock(PubSubDeserializer.class);
        when(deserializer.deserialize(eq("test-topic"), any(), eq(KnownEvent.class))).thenReturn(knownEvent);

        var received = new ArrayList<KnownEvent>();
        var subscriptionRequest = new SubscriptionRequest<>("test-topic", "sub", KnownEvent.class, received::add)
                .withExecutor(Runnable::run);

        var unknownTypeMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8("{}"))
                .putAttributes("type", "com.example.UnknownEvent")
                .build();
        var knownTypeMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8("{}"))
                .putAttributes("type", KnownEvent.class.getName())
                .build();

        var util = pubSubUtil(eventRegistry, deserializer);
        util.consumeTyped(subscriptionRequest, unknownTypeMessage);
        util.consumeTyped(subscriptionRequest, knownTypeMessage);

        assertEquals(List.of(knownEvent), received);
    }

    private static PubSubUtil pubSubUtil(EventRegistry eventRegistry) {
        return pubSubUtil(eventRegistry, UNUSED_DESERIALIZER);
    }

    private static PubSubUtil pubSubUtil(EventRegistry eventRegistry, PubSubDeserializer deserializer) {
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
                deserializer,
                eventRegistry
        );
    }

    private sealed interface ParentEvent permits ChildEvent {}

    private static final class ChildEvent implements ParentEvent {}

    private static final class AmbiguousEvent {}

    private static final class UnmappedEvent {}

    private record KnownEvent(String value) {}
}
