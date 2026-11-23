package be.appify.prefab.streams.snssqs;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsMessageMetadata;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.streams.StreamKeyEncoding;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSink;
import be.appify.prefab.streams.tracing.MessageTracing;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes the outbox records of the engine to SNS.
 *
 * <p>On a FIFO topic the message group is the record key, which keeps all records of a key in order, and the
 * deduplication ID is the {@code prefab-message-id}, so a record that the relay publishes twice is delivered once. A
 * message carries three attributes of its own, {@code prefab-message-id}, {@code prefab-event-time} and
 * {@code prefab-correlation-ids}. With the event type, the two version attributes of an event and the two attributes
 * of a tombstone that makes at most eight, within the ten that SQS delivers.
 */
public final class SnsSqsStreamSink implements StreamSink {
    private static final String DEFAULT_MESSAGE_GROUP = "prefab-default";
    /** SNS refuses an empty message, so a tombstone carries this placeholder next to its tombstone attribute. */
    private static final String TOMBSTONE_PAYLOAD = "null";

    private final SnsTemplate snsTemplate;
    private final SqsUtil sqsUtil;
    private final SnsSerializer serializer;
    private final EventRegistry eventRegistry;
    private final Map<String, String> topicArns = new ConcurrentHashMap<>();

    /**
     * Creates a sink.
     *
     * @param snsTemplate publishes notifications
     * @param sqsUtil creates topics
     * @param serializer serializes payloads in the format of their topic
     * @param eventRegistry registry of the events, which tells which events declare a partitioning key
     */
    public SnsSqsStreamSink(
            SnsTemplate snsTemplate,
            SqsUtil sqsUtil,
            SnsSerializer serializer,
            EventRegistry eventRegistry
    ) {
        this.snsTemplate = snsTemplate;
        this.sqsUtil = sqsUtil;
        this.serializer = serializer;
        this.eventRegistry = eventRegistry;
    }

    @Override
    public void publish(String topic, StreamMessage message) {
        var publication = publication(topic, message);
        var fifo = topic.endsWith(".fifo") || isPartitioned(message, publication);
        var topicArn = topicArns.computeIfAbsent(
                (fifo && !topic.endsWith(".fifo") ? topic + ".fifo" : topic),
                ignored -> sqsUtil.ensureTopicExists(topic, fifo));
        var tombstone = message.value() == null;
        var notification = SnsNotification.builder(tombstone ? TOMBSTONE_PAYLOAD : serializer.serialize(topic, publication))
                .headers(Map.copyOf(attributes(message, publication, tombstone)));
        subject(message).ifPresent(notification::subject);
        if (fifo) {
            notification.groupId(groupId(message)).deduplicationId(message.messageId());
        }
        snsTemplate.sendNotification(topicArn, notification.build());
    }

    private Object publication(String topic, StreamMessage message) {
        var identity = message.headers().get(EventType.HEADER);
        if (identity == null || message.value() == null || !eventRegistry.hasConcreteTypes(topic)) {
            return message.value();
        }
        @SuppressWarnings("unchecked")
        var type = (EventType<Object>) eventRegistry.concreteTypeFor(topic, identity);
        return new TypedEvent<>(type, message.value());
    }

    private boolean isPartitioned(StreamMessage message, Object publication) {
        return message.value() != null && eventRegistry.hasPartitioningKey(publication);
    }

    private Map<String, String> attributes(StreamMessage message, Object publication, boolean tombstone) {
        var attributes = new LinkedHashMap<String, String>();
        attributes.put(MessageTracing.MESSAGE_ID_HEADER, message.messageId());
        attributes.put(SnsSqsStreamSource.EVENT_TIME_ATTRIBUTE, message.eventTime().toString());
        copy(message, MessageTracing.CORRELATION_IDS_HEADER, attributes);
        copy(message, EventType.HEADER, attributes);
        if (message.value() != null) {
            attributes.putAll(EventVersions.headersFor(publication));
        }
        if (tombstone) {
            attributes.put(SqsMessageMetadata.TOMBSTONE_ATTRIBUTE, "true");
            attributes.put(SnsSqsStreamSource.KEY_ATTRIBUTE, encode(message.key()));
        }
        return attributes;
    }

    private static void copy(StreamMessage message, String name, Map<String, String> attributes) {
        var value = message.headers().get(name);
        if (value != null) {
            attributes.put(name, value);
        }
    }

    /** The subject carries the class name that the reader uses to pick a subtype, when the class is known to it. */
    private Optional<String> subject(StreamMessage message) {
        return Optional.ofNullable(message.value())
                .map(value -> value.getClass().getName())
                .filter(name -> eventRegistry.typeByClassName(name).isPresent());
    }

    private static String groupId(StreamMessage message) {
        return message.key() == null ? DEFAULT_MESSAGE_GROUP : SnsSqsNames.messageGroupId(encode(message.key()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String encode(Object key) {
        if (key == null) {
            throw new IllegalStateException("A message without a payload needs a key");
        }
        if (key instanceof String text) {
            return text;
        }
        return new String(new StreamKeyEncoding(key.getClass()).encode(key), StandardCharsets.UTF_8);
    }
}
