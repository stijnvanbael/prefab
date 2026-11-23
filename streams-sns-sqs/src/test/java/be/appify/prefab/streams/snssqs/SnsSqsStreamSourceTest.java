package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.sns.SqsMessageConsumer;
import be.appify.prefab.core.sns.SqsMessageMetadata;
import be.appify.prefab.core.sns.SqsSubscription;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSource.SourceBinding;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SnsSqsStreamSourceTest {
    private static final Instant SENT = Instant.parse("2026-01-01T10:00:00Z");

    private final SqsUtil sqsUtil = mock(SqsUtil.class);
    private final EventRegistry registry = new EventRegistry();
    private final SnsSqsStreamSource source =
            new SnsSqsStreamSource(sqsUtil, registry, "shop", Duration.ofMinutes(3));
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ArrayList<StreamMessage> received = new ArrayList<>();
    private final ArrayList<Boolean> acknowledged = new ArrayList<>();

    @BeforeEach
    void subscriptionHandle() {
        when(sqsUtil.subscribe(any())).thenReturn(() -> closed.set(true));
    }

    @Test
    void subscribesOneQueuePerSourceNodeWithTheConfiguredVisibilityTimeout() {
        var request = subscribe(binding("source-sales", "sales"));

        assertThat(request.topic()).isEqualTo("sales");
        assertThat(request.queueName()).isEqualTo("shop-streams-source-sales");
        assertThat(request.type()).isEqualTo(Sale.class);
        assertThat(request.visibilityTimeout()).contains(Duration.ofMinutes(3));
        assertThat(request.fifo()).isFalse();
    }

    @Test
    void usesFifoWhenTheTopicIsFifo() {
        var request = subscribe(binding("source-sales", "sales.fifo"));

        assertThat(request.fifo()).isTrue();
        assertThat(source.ordered("sales.fifo")).isTrue();
    }

    @Test
    void usesFifoWhenTheEventDeclaresAPartitioningKey() {
        registry.register("sales", Sale.class, Event.Serialization.JSON, Sale::saleId);

        var request = subscribe(binding("source-sales", "sales"));

        assertThat(request.fifo()).isTrue();
        assertThat(source.ordered("sales")).isTrue();
    }

    @Test
    void isUnorderedWithoutFifo() {
        assertThat(source.ordered("sales")).isFalse();
    }

    @Test
    void takesMessageIdAndEventTimeFromTheAttributes() {
        var consumer = subscribe(binding("source-sales", "sales")).messageConsumer().orElseThrow();

        consumer.accept(new Sale("s-1", "books", 5), metadata(Map.of(
                "prefab-message-id", "m-1",
                "prefab-event-time", "2026-01-01T09:00:00Z",
                "prefab-correlation-ids", "a,b")), () -> acknowledged.add(true));

        var message = received.getFirst();
        assertThat(message.messageId()).isEqualTo("m-1");
        assertThat(message.key()).isEqualTo("s-1");
        assertThat(message.value()).isEqualTo(new Sale("s-1", "books", 5));
        assertThat(message.eventTime()).isEqualTo(Instant.parse("2026-01-01T09:00:00Z"));
        assertThat(message.headers()).containsEntry("prefab-correlation-ids", "a,b");
    }

    @Test
    void fallsBackToTheSqsMessageIdAndSentTimestamp() {
        var consumer = subscribe(binding("source-sales", "sales")).messageConsumer().orElseThrow();

        consumer.accept(new Sale("s-1", "books", 5), metadata(Map.of()), () -> acknowledged.add(true));

        var message = received.getFirst();
        assertThat(message.messageId()).isEqualTo("sqs-1");
        assertThat(message.eventTime()).isEqualTo(SENT);
    }

    @Test
    void theMessageIsDeletedOnlyWhenTheEngineAcknowledges() {
        var consumer = subscribe(binding("source-sales", "sales")).messageConsumer().orElseThrow();
        var deleted = new AtomicBoolean();

        consumer.accept(new Sale("s-1", "books", 5), metadata(Map.of()), () -> deleted.set(true));

        assertThat(deleted).isFalse();
        acknowledgements.getFirst().acknowledge();
        assertThat(deleted).isTrue();
    }

    @Test
    void aTombstoneTakesItsKeyFromTheKeyAttribute() {
        var consumer = subscribe(binding("source-sales", "sales")).messageConsumer().orElseThrow();

        consumer.accept(null, metadata(Map.of("prefab-tombstone", "true", "prefab-key", "s-9")), () -> { });

        var message = received.getFirst();
        assertThat(message.key()).isEqualTo("s-9");
        assertThat(message.value()).isNull();
    }

    @Test
    void aMessageWithoutPayloadAndKeyIsRejected() {
        var consumer = subscribe(binding("source-sales", "sales")).messageConsumer().orElseThrow();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> consumer.accept(null, metadata(Map.of("prefab-tombstone", "true")), () -> { }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prefab-key");
    }

    @Test
    void closingTheSubscriptionStopsPolling() {
        var subscription = source.subscribe(binding("source-sales", "sales"), (message, ack) -> { });

        subscription.close();

        assertThat(closed).isTrue();
    }

    @Test
    void subscribingByTopicAloneIsNotSupported() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> source.subscribe("sales", (message, ack) -> { }))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private final ArrayList<SqsMessageConsumer.Acknowledgement> acknowledgements = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private SqsSubscriptionRequest<Object> subscribe(SourceBinding binding) {
        source.subscribe(binding, (message, acknowledgement) -> {
            received.add(message);
            acknowledgements.add(acknowledgement::acknowledge);
        });
        var captor = ArgumentCaptor.forClass(SqsSubscriptionRequest.class);
        verify(sqsUtil).subscribe(captor.capture());
        return captor.getValue();
    }

    private static SourceBinding binding(String name, String topic) {
        return new SourceBinding(name, topic, Sale.class, null);
    }

    private static SqsMessageMetadata metadata(Map<String, String> attributes) {
        return new SqsMessageMetadata("sqs-1", attributes, SENT, Optional.empty());
    }
}
