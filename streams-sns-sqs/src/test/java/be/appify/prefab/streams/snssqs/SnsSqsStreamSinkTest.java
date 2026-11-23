package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.snssqs.StreamFixtures.CategoryTotal;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SnsSqsStreamSinkTest {
    private static final Instant EVENT_TIME = Instant.parse("2026-01-01T09:00:00Z");

    private final SnsTemplate snsTemplate = mock(SnsTemplate.class);
    private final SqsUtil sqsUtil = mock(SqsUtil.class);
    private final SnsSerializer serializer = mock(SnsSerializer.class);
    private final EventRegistry registry = new EventRegistry();
    private final SnsSqsStreamSink sink = new SnsSqsStreamSink(snsTemplate, sqsUtil, serializer, registry);

    @BeforeEach
    void brokerDefaults() {
        when(sqsUtil.ensureTopicExists(anyString(), anyBoolean())).thenAnswer(call -> "arn:" + call.getArgument(0));
        when(serializer.serialize(anyString(), any())).thenReturn("{\"json\":true}");
    }

    @Test
    void publishesTheSerializedPayloadWithTheTraceAttributes() {
        sink.publish("totals", new StreamMessage("m-1", "books", new CategoryTotal("books", 5),
                Map.of("prefab-correlation-ids", "a,b", "ignored", "x"), EVENT_TIME));

        var notification = published("arn:totals");
        assertThat(notification.getPayload()).isEqualTo("{\"json\":true}");
        assertThat(notification.getHeaders())
                .containsEntry("prefab-message-id", "m-1")
                .containsEntry("prefab-event-time", "2026-01-01T09:00:00Z")
                .containsEntry("prefab-correlation-ids", "a,b")
                .doesNotContainKey("ignored");
        assertThat(notification.getGroupId()).isNull();
        assertThat(notification.getDeduplicationId()).isNull();
    }

    @Test
    void usesAtMostThreeAttributesOfItsOwn() {
        sink.publish("totals", new StreamMessage("m-1", "books", new CategoryTotal("books", 5),
                Map.of("prefab-correlation-ids", "a"), EVENT_TIME));

        assertThat(published("arn:totals").getHeaders().keySet())
                .containsExactlyInAnyOrder("prefab-message-id", "prefab-event-time", "prefab-correlation-ids");
    }

    @Test
    void fifoTopicsGetTheRecordKeyAsGroupAndTheMessageIdAsDeduplicationId() {
        sink.publish("totals.fifo", new StreamMessage("m-1", "books", new CategoryTotal("books", 5), Map.of(),
                EVENT_TIME));

        var notification = published("arn:totals.fifo");
        assertThat(notification.getGroupId()).isEqualTo("books");
        assertThat(notification.getDeduplicationId()).isEqualTo("m-1");
        verify(sqsUtil).ensureTopicExists("totals.fifo", true);
    }

    @Test
    void aTopicOfAnEventWithAPartitioningKeyIsFifo() {
        registry.register("totals", CategoryTotal.class, Event.Serialization.JSON, CategoryTotal::category);

        sink.publish("totals", new StreamMessage("m-1", "books", new CategoryTotal("books", 5), Map.of(),
                EVENT_TIME));

        verify(sqsUtil).ensureTopicExists("totals", true);
        assertThat(published("arn:totals").getGroupId()).isEqualTo("books");
    }

    @Test
    void aKeyRecordIsEncodedLikeOnKafka() {
        sink.publish("totals.fifo", new StreamMessage("m-1", new Key("books"), new CategoryTotal("books", 5),
                Map.of(), EVENT_TIME));

        assertThat(published("arn:totals.fifo").getGroupId()).isEqualTo("books");
    }

    @Test
    void createsTheTopicOnlyOnce() {
        var message = new StreamMessage("m-1", "books", new CategoryTotal("books", 5), Map.of(), EVENT_TIME);

        sink.publish("totals", message);
        sink.publish("totals", message);

        verify(sqsUtil, times(1)).ensureTopicExists("totals", false);
        verify(snsTemplate, times(2)).sendNotification(eq("arn:totals"), any(SnsNotification.class));
    }

    @Test
    void theSubjectIsOnlySetForAKnownEventClass() {
        registry.register("sales", Sale.class, Event.Serialization.JSON);

        sink.publish("sales", new StreamMessage("m-1", "s-1", new Sale("s-1", "books", 5), Map.of(), EVENT_TIME));
        sink.publish("totals", new StreamMessage("m-2", "books", new CategoryTotal("books", 5), Map.of(), EVENT_TIME));

        assertThat(published("arn:sales").getSubject()).isEqualTo(Sale.class.getName());
        assertThat(published("arn:totals").getSubject()).isNull();
    }

    @Test
    void aTombstoneCarriesItsKeyAndAPlaceholderPayload() {
        sink.publish("totals", new StreamMessage("m-1", "books", null, Map.of(), EVENT_TIME));

        var notification = published("arn:totals");
        assertThat(notification.getPayload()).isEqualTo("null");
        assertThat(notification.getHeaders())
                .containsEntry("prefab-tombstone", "true")
                .containsEntry("prefab-key", "books");
    }

    @Test
    void aTombstoneNeedsAKey() {
        var tombstone = new StreamMessage("m-1", null, null, Map.of(), EVENT_TIME);

        assertThatThrownBy(() -> sink.publish("totals", tombstone))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("needs a key");
    }

    @SuppressWarnings("unchecked")
    private SnsNotification<String> published(String topicArn) {
        var captor = ArgumentCaptor.forClass(SnsNotification.class);
        verify(snsTemplate, org.mockito.Mockito.atLeastOnce()).sendNotification(eq(topicArn), captor.capture());
        return captor.getValue();
    }

    private record Key(String id) {
    }
}
