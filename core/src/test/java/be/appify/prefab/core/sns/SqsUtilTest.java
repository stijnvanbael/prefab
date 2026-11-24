package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void ensureTopicExistsCreatesFifoTopicWhenPartitioningIsEnabled() {
        var snsClient = mock(SnsClient.class);
        when(snsClient.createTopic(any(CreateTopicRequest.class))).thenReturn(
                CreateTopicResponse.builder().topicArn("arn:aws:sns:us-east-1:000000000000:orders.fifo").build());
        var sqsUtil = new SqsUtil(
                "app", "", 5, 1000, 30000, 1.5, snsClient, UNUSED_SQS_CLIENT, UNUSED_SQS_DESERIALIZER,
                new EventRegistry());

        assertEquals("arn:aws:sns:us-east-1:000000000000:orders.fifo", sqsUtil.ensureTopicExists("orders", true));
        verify(snsClient).createTopic(argThat((CreateTopicRequest request) ->
                request.name().equals("orders.fifo")
                        && request.attributes().get("FifoTopic").equals("true")
                        && request.attributes().get("ContentBasedDeduplication").equals("false")));
    }

    @ParameterizedTest
    @CsvSource({
            "showcase.customer-registered, false, showcase-customer-registered",
            "showcase.order, true, showcase-order.fifo",
            "showcase.order.fifo, false, showcase-order.fifo",
            "orders_v2-eu, false, orders_v2-eu"})
    void ensureTopicExistsReplacesCharactersThatSnsDoesNotAllowInTopicNames(
            String topic, boolean fifo, String expectedName
    ) {
        var snsClient = mock(SnsClient.class);
        when(snsClient.createTopic(any(CreateTopicRequest.class))).thenReturn(
                CreateTopicResponse.builder().topicArn("arn:aws:sns:us-east-1:000000000000:" + expectedName).build());
        var sqsUtil = new SqsUtil(
                "app", "", 5, 1000, 30000, 1.5, snsClient, UNUSED_SQS_CLIENT, UNUSED_SQS_DESERIALIZER,
                new EventRegistry());

        sqsUtil.ensureTopicExists(topic, fifo);

        verify(snsClient).createTopic(argThat((CreateTopicRequest request) -> request.name().equals(expectedName)));
    }

    @Test
    void metadataConsumerDecidesWhenTheMessageIsDeleted() {
        var sqsClient = deletingClient();
        var deserializer = deserializerFor("{\"value\":1}", Map.of("prefab-message-id", "m-1"));
        when(deserializer.deserialize("topic", "body", UnmappedEvent.class)).thenReturn(new UnmappedEvent());
        var sqsUtil = sqsUtil(sqsClient, deserializer);
        var received = new ArrayList<SqsMessageMetadata>();
        var acknowledgements = new ArrayList<SqsMessageConsumer.Acknowledgement>();
        var request = new SqsSubscriptionRequest<>("topic", "queue", UnmappedEvent.class, event -> {
        }).withMessageConsumer((event, metadata, acknowledgement) -> {
            received.add(metadata);
            acknowledgements.add(acknowledgement);
        });

        sqsUtil.processMessage(request, "queue-url", message("body", "1700000000000", "group-7"));

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        acknowledgements.getFirst().acknowledge();
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        var metadata = received.getFirst();
        assertEquals("sqs-1", metadata.messageId());
        assertEquals("m-1", metadata.attributes().get("prefab-message-id"));
        assertEquals(java.time.Instant.ofEpochMilli(1_700_000_000_000L), metadata.sentTimestamp());
        assertEquals(Optional.of("group-7"), metadata.messageGroupId());
    }

    @Test
    void tombstoneIsDeliveredWithoutDeserializingThePayload() {
        var sqsClient = deletingClient();
        var deserializer = deserializerFor("null", Map.of(SqsMessageMetadata.TOMBSTONE_ATTRIBUTE, "true"));
        var sqsUtil = sqsUtil(sqsClient, deserializer);
        var events = new ArrayList<UnmappedEvent>();
        var request = new SqsSubscriptionRequest<>("topic", "queue", UnmappedEvent.class, event -> {
        }).withMessageConsumer((event, metadata, acknowledgement) -> {
            events.add(event);
            assertTrue(metadata.tombstone());
            acknowledgement.acknowledge();
        });

        sqsUtil.processMessage(request, "queue-url", message("null", "1700000000000", "group-7"));

        assertEquals(1, events.size());
        assertNull(events.getFirst());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        verify(deserializer, never()).deserialize(anyString(), anyString(), any());
    }

    private static SqsAsyncClient deletingClient() {
        var sqsClient = mock(SqsAsyncClient.class);
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));
        return sqsClient;
    }

    private static SqsDeserializer deserializerFor(String payload, Map<String, String> attributes) {
        var deserializer = mock(SqsDeserializer.class);
        when(deserializer.parseNotification(anyString()))
                .thenReturn(new SqsNotification(payload, null, attributes));
        return deserializer;
    }

    private static Message message(String body, String sentTimestamp, String groupId) {
        return Message.builder()
                .messageId("sqs-1")
                .receiptHandle("receipt")
                .body(body)
                .attributes(Map.of(
                        MessageSystemAttributeName.SENT_TIMESTAMP, sentTimestamp,
                        MessageSystemAttributeName.MESSAGE_GROUP_ID, groupId))
                .build();
    }

    private static SqsUtil sqsUtil(SqsAsyncClient sqsClient, SqsDeserializer deserializer) {
        return new SqsUtil("app", "", 5, 1, 1, 1.0, UNUSED_SNS_CLIENT, sqsClient, deserializer, new EventRegistry());
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
