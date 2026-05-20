package be.appify.prefab.core.sns;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SqsUtilTest {

    @Test
    void tryTopicForTypeReturnsEmptyWhenNoTopicMatches() {
        var sqsUtil = sqsUtil();

        assertEquals(Optional.empty(), sqsUtil.tryTopicForType(UnmappedEvent.class));
    }

    @Test
    void topicForTypePrefersMostSpecificAssignableType() {
        var sqsUtil = sqsUtil();
        sqsUtil.registerEventTopic("parent-topic", ParentEvent.class);
        sqsUtil.registerEventTopic("base-topic", BaseEvent.class);

        assertEquals("parent-topic", sqsUtil.topicForType(ChildEvent.class));
    }

    @Test
    void topicForTypeThrowsOnAmbiguousAssignableTypes() {
        var sqsUtil = sqsUtil();
        sqsUtil.registerEventTopic("left-topic", LeftEvent.class);
        sqsUtil.registerEventTopic("right-topic", RightEvent.class);

        assertThrows(IllegalStateException.class, () -> sqsUtil.topicForType(AmbiguousEvent.class));
    }

    private static SqsUtil sqsUtil() {
        return new SqsUtil(
                "app",
                "",
                5,
                1000,
                30000,
                1.5,
                mock(SnsClient.class),
                mock(SqsAsyncClient.class),
                mock(SqsDeserializer.class)
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
