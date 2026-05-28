package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqsUtilTest {
    private static final SnsClient UNUSED_SNS_CLIENT = null;
    private static final SqsAsyncClient UNUSED_SQS_CLIENT = null;
    private static final SqsDeserializer UNUSED_SQS_DESERIALIZER = null;

    @Test
    void tryTopicForTypeReturnsEmptyWhenNoTopicMatches() {
        var sqsUtil = sqsUtil(new EventRegistry());

        assertEquals(Optional.empty(), sqsUtil.tryTopicForType(UnmappedEvent.class));
    }

    @Test
    void topicForTypeResolvesPermittedSealedSubtype() {
        var eventRegistry = new EventRegistry();
        var sqsUtil = sqsUtil(eventRegistry);
        eventRegistry.register("parent-topic", ParentEvent.class, Event.Serialization.JSON);

        // ChildEvent is a permitted subtype of sealed ParentEvent — registered automatically
        assertEquals("parent-topic", sqsUtil.topicForType(ChildEvent.class));
    }

    @Test
    void topicForTypeThrowsWhenMultipleTopicsRegistered() {
        var eventRegistry = new EventRegistry();
        var sqsUtil = sqsUtil(eventRegistry);
        eventRegistry.register("topic1", AmbiguousEvent.class, Event.Serialization.JSON);
        eventRegistry.register("topic2", AmbiguousEvent.class, Event.Serialization.JSON);

        assertThrows(IllegalStateException.class, () -> sqsUtil.topicForType(AmbiguousEvent.class));
    }

    private static SqsUtil sqsUtil(EventRegistry eventRegistry) {
        return new SqsUtil(
                "app",
                "",
                5,
                1000,
                30000,
                1.5,
                UNUSED_SNS_CLIENT,
                UNUSED_SQS_CLIENT,
                UNUSED_SQS_DESERIALIZER,
                eventRegistry
        );
    }

    private sealed interface ParentEvent permits ChildEvent {}

    private static final class ChildEvent implements ParentEvent {}

    private static final class AmbiguousEvent {}

    private static final class UnmappedEvent {}
}
