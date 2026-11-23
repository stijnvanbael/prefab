package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSource;
import be.appify.prefab.streams.tracing.MessageTracing;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PubSubStreamSourceTest {
    @Event(topic = "input")
    record Entry(String id) implements Keyed<String> {
        @Override public String key() { return id; }
    }

    @Event(topic = "shared", typeArguments = String.class)
    @Event(topic = "shared", typeArguments = Integer.class)
    record Changed<T>(String id, T value) implements Keyed<String> {
        @Override public String key() { return id; }
    }

    sealed interface Base extends Keyed<String> permits Created {}
    record Created(String id) implements Base {
        @Override public String key() { return id; }
    }

    private final PubSubUtil util = mock(PubSubUtil.class);
    private final PubSubDeserializer deserializer = mock(PubSubDeserializer.class);
    private final EventRegistry registry = new EventRegistry();
    private final PubSubStreamSource source = new PubSubStreamSource("shop", util, deserializer, registry);

    @Test
    void deliversMetadataAndUsesPublishTimeWhenEventTimeIsAbsent() {
        registry.register("input", Entry.class, Event.Serialization.JSON);
        var binding = new StreamSource.SourceBinding("source-test", "input", Entry.class, null);
        var input = PubsubMessage.newBuilder().setMessageId("broker-id").setData(ByteString.copyFromUtf8("{}"))
                .setPublishTime(Timestamp.newBuilder().setSeconds(100).setNanos(123)).build();
        when(deserializer.deserialize(eq("input"), any(), eq(Entry.class), any())).thenReturn(new Entry("key"));
        var received = new ArrayList<StreamMessage>();
        var request = subscribe(binding, (message, ack) -> { received.add(message); ack.acknowledge(); });

        request.rawConsumer().orElseThrow().accept(input);

        assertThat(request.subscription()).isEqualTo("shop.streams.source-test");
        assertThat(received).containsExactly(new StreamMessage("broker-id", "key", new Entry("key"),
                input.getAttributesMap(), Instant.ofEpochSecond(100, 123)));
    }

    @Test
    void usesPrefabIdentityAndEventTimeAndRequiresEngineAcknowledgement() {
        registry.register("input", Entry.class, Event.Serialization.JSON);
        var binding = new StreamSource.SourceBinding("source-test", "input", Entry.class, null);
        var input = PubsubMessage.newBuilder().setMessageId("broker-id").setData(ByteString.copyFromUtf8("{}"))
                .putAttributes(MessageTracing.MESSAGE_ID_HEADER, "prefab-id")
                .putAttributes(PubSubStreamSource.EVENT_TIME, "2026-01-01T00:00:00Z").build();
        when(deserializer.deserialize(eq("input"), any(), eq(Entry.class), any())).thenReturn(new Entry("key"));
        var received = new ArrayList<StreamMessage>();
        var request = subscribe(binding, (message, ack) -> received.add(message));

        assertThatThrownBy(() -> request.rawConsumer().orElseThrow().accept(input))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("did not acknowledge");
        assertThat(received.getFirst().messageId()).isEqualTo("prefab-id");
        assertThat(received.getFirst().eventTime()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void filtersOtherConcreteBindingsAndDecodesTombstones() {
        EventType<Changed<String>> strings = new EventType<>() {};
        EventType<Changed<Integer>> numbers = new EventType<>() {};
        registry.register("shared", strings, Event.Serialization.JSON);
        registry.register("shared", numbers, Event.Serialization.JSON);
        var binding = new StreamSource.SourceBinding("global-values", "shared", Changed.class, strings.id());
        var received = new ArrayList<StreamMessage>();
        var request = subscribe(binding, (message, ack) -> { received.add(message); ack.acknowledge(); });
        var message = PubsubMessage.newBuilder().setMessageId("id").setOrderingKey("key")
                .putAttributes(PubSubStreamSource.EVENT_TIME, "2026-01-01T00:00:00Z")
                .putAttributes(PubSubStreamSource.TOMBSTONE, "true")
                .putAttributes(EventType.HEADER, numbers.id()).build();

        request.rawConsumer().orElseThrow().accept(message);
        request.rawConsumer().orElseThrow().accept(message.toBuilder().putAttributes(EventType.HEADER, strings.id()).build());

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().key()).isEqualTo("key");
        assertThat(received.getFirst().value()).isNull();
    }

    @Test
    void deserializesRegisteredSubtypesForAbstractSourceBindings() {
        registry.register("shared", Base.class, Event.Serialization.JSON);
        registry.register("shared", Created.class, Event.Serialization.JSON);
        var binding = new StreamSource.SourceBinding("source-base", "shared", Base.class, null);
        var input = PubsubMessage.newBuilder().setMessageId("id").setData(ByteString.copyFromUtf8("{}"))
                .putAttributes("type", Created.class.getName())
                .putAttributes(PubSubStreamSource.EVENT_TIME, "2026-01-01T00:00:00Z").build();
        when(deserializer.deserialize(eq("shared"), any(), eq(Created.class), any())).thenReturn(new Created("key"));
        var received = new ArrayList<StreamMessage>();
        var request = subscribe(binding, (message, ack) -> { received.add(message); ack.acknowledge(); });

        request.rawConsumer().orElseThrow().accept(input);

        assertThat(received).singleElement().extracting(StreamMessage::value).isEqualTo(new Created("key"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SubscriptionRequest<PubsubMessage> subscribe(StreamSource.SourceBinding binding, StreamSource.MessageHandler handler) {
        var subscriber = mock(Subscriber.class);
        when(util.subscribeWithHandle(any())).thenReturn(subscriber);
        source.subscribe(binding, handler);
        var captor = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(util).subscribeWithHandle(captor.capture());
        return captor.getValue();
    }
}
