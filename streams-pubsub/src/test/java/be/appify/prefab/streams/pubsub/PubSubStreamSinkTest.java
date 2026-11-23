package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.tracing.MessageTracing;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.pubsub.v1.PubsubMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PubSubStreamSinkTest {
    @Event(topic = "output")
    record Result(String id) implements Keyed<String> {
        @Override public String key() { return id; }
    }

    private final PubSubUtil util = mock(PubSubUtil.class);
    private final PubSubSerializer serializer = mock(PubSubSerializer.class);
    private final EventRegistry registry = new EventRegistry();
    private final PublisherFactory factory = mock(PublisherFactory.class);
    private final Publisher publisher = mock(Publisher.class);
    private final PubSubStreamSink sink = new PubSubStreamSink(util, serializer, registry, factory);

    PubSubStreamSinkTest() {
        registry.register("output", Result.class, Event.Serialization.JSON);
        when(util.ensureTopicExists("output")).thenReturn("projects/project/topics/output");
        when(factory.createPublisher("projects/project/topics/output")).thenReturn(publisher);
        when(serializer.serialize(eq("output"), any())).thenReturn("payload".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void publishesOrderingKeyEventTimeAndNewestCorrelationIds() {
        when(publisher.publish(any())).thenReturn(ApiFutures.immediateFuture("broker-id"));
        var old = "a".repeat(600);
        var recent = "b".repeat(600);
        var message = new StreamMessage("message-id", "key", new Result("key"),
                Map.of(MessageTracing.CORRELATION_IDS_HEADER, old + "," + recent),
                Instant.parse("2026-01-01T00:00:00Z"));

        sink.publish("output", message);

        var captor = ArgumentCaptor.forClass(PubsubMessage.class);
        verify(publisher).publish(captor.capture());
        var result = captor.getValue();
        assertThat(result.getOrderingKey()).isEqualTo("key");
        assertThat(result.getData().toStringUtf8()).isEqualTo("payload");
        assertThat(result.getAttributesMap())
                .containsEntry(MessageTracing.MESSAGE_ID_HEADER, "message-id")
                .containsEntry(MessageTracing.CORRELATION_IDS_HEADER, recent)
                .containsEntry(PubSubStreamSource.EVENT_TIME, "2026-01-01T00:00:00Z");
    }

    @Test
    void resumesOrderingKeyAfterPublishFailure() {
        when(publisher.publish(any())).thenReturn(ApiFutures.immediateFailedFuture(new IllegalStateException("failure")));
        var message = new StreamMessage("id", "key", new Result("key"), Map.of(), Instant.EPOCH);

        assertThatThrownBy(() -> sink.publish("output", message))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Failed to publish")
                .hasCauseInstanceOf(java.util.concurrent.ExecutionException.class);
        verify(publisher).resumePublish("key");
    }
}
