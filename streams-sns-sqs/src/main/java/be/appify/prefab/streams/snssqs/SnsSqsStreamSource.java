package be.appify.prefab.streams.snssqs;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.sns.SqsMessageMetadata;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.streams.StreamKeyEncoding;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSource;
import be.appify.prefab.streams.tracing.MessageTracing;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.core.ResolvableType;

/**
 * Reads the sources of a topology from SQS.
 *
 * <p>Every source gets its own queue, {@code {application}-streams-{source-name}}, subscribed to the source topic and
 * shared by all instances of the application. The queue is FIFO when the source is: the topic ends in
 * {@code .fifo} or its events declare a partitioning key. A message is deleted only when the engine acknowledges it,
 * which is after the transaction that processed it committed.
 */
public final class SnsSqsStreamSource implements StreamSource {
    /** Attribute that carries the encoded key of a tombstone, which has no payload to take the key from. */
    static final String KEY_ATTRIBUTE = "prefab-key";
    /** Attribute that carries the event time of a message. */
    static final String EVENT_TIME_ATTRIBUTE = "prefab-event-time";

    private final SqsUtil sqsUtil;
    private final EventRegistry eventRegistry;
    private final String application;
    private final Duration visibilityTimeout;

    /**
     * Creates a source.
     *
     * @param sqsUtil provisions queues and polls them
     * @param eventRegistry registry of the events, which tells which events declare a partitioning key
     * @param application name of the application, the prefix of the queue names
     * @param visibilityTimeout visibility timeout of the queues
     */
    public SnsSqsStreamSource(
            SqsUtil sqsUtil,
            EventRegistry eventRegistry,
            String application,
            Duration visibilityTimeout
    ) {
        this.sqsUtil = sqsUtil;
        this.eventRegistry = eventRegistry;
        this.application = application;
        this.visibilityTimeout = visibilityTimeout;
    }

    @Override
    public boolean ordered(String topic) {
        return topic.endsWith(".fifo")
                || eventRegistry.registeredTypesForTopic(topic).stream().anyMatch(eventRegistry::hasPartitioningKey);
    }

    @Override
    public Subscription subscribe(String topic, MessageHandler handler) {
        throw new UnsupportedOperationException(
                "The SNS/SQS source needs the value type of a topic; subscribe with a SourceBinding");
    }

    @Override
    public Subscription subscribe(SourceBinding binding, MessageHandler handler) {
        var subscription = sqsUtil.subscribe(request(binding, handler));
        return subscription::close;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SqsSubscriptionRequest<Object> request(SourceBinding binding, MessageHandler handler) {
        var queueName = SnsSqsNames.queueName(application, binding.name());
        var eventType = eventType(binding);
        var request = eventType == null
                ? new SqsSubscriptionRequest<Object>(binding.topic(), queueName,
                        (Class<Object>) binding.valueType(), event -> { })
                : new SqsSubscriptionRequest<Object>(binding.topic(), queueName,
                        (EventType<Object>) (EventType) eventType, event -> { });
        return request
                .withFifo(fifo(binding, eventType))
                .withVisibilityTimeout(visibilityTimeout)
                .withMessageConsumer((event, metadata, acknowledgement) ->
                        handler.onMessage(message(binding, eventType, event, metadata), acknowledgement::acknowledge));
    }

    /** Resolves the concrete generic binding of a source, which the registry knows by topic and identity. */
    private EventType<?> eventType(SourceBinding binding) {
        return binding.eventTypeId() == null
                ? null : eventRegistry.concreteTypeFor(binding.topic(), binding.eventTypeId());
    }

    private boolean fifo(SourceBinding binding, EventType<?> eventType) {
        if (binding.topic().endsWith(".fifo")) {
            return true;
        }
        return eventType != null
                ? eventRegistry.hasPartitioningKey(eventType)
                : eventRegistry.hasPartitioningKey(binding.valueType());
    }

    private StreamMessage message(
            SourceBinding binding,
            EventType<?> eventType,
            Object event,
            SqsMessageMetadata metadata
    ) {
        var attributes = metadata.attributes();
        var messageId = attributes.getOrDefault(MessageTracing.MESSAGE_ID_HEADER, metadata.messageId());
        return new StreamMessage(messageId, key(binding, eventType, event, attributes), event, attributes,
                eventTime(attributes, metadata));
    }

    private Object key(SourceBinding binding, EventType<?> eventType, Object event, Map<String, String> attributes) {
        if (event instanceof Keyed<?> keyed) {
            return keyed.key();
        }
        var encoded = attributes.get(KEY_ATTRIBUTE);
        if (encoded == null) {
            throw new IllegalStateException(
                    "Message without a payload on topic '%s' needs the attribute %s with its key"
                            .formatted(binding.topic(), KEY_ATTRIBUTE));
        }
        return decodeKey(keyType(binding, eventType), encoded);
    }

    @SuppressWarnings("unchecked")
    private static <K> K decodeKey(Class<K> keyType, String encoded) {
        if (keyType == String.class) {
            return (K) encoded;
        }
        return new StreamKeyEncoding<>(keyType).decode(encoded.getBytes(StandardCharsets.UTF_8));
    }

    private static Class<?> keyType(SourceBinding binding, EventType<?> eventType) {
        var type = eventType != null
                ? ResolvableType.forType(eventType.type())
                : ResolvableType.forClass(binding.valueType());
        var keyType = type.as(Keyed.class).getGeneric(0).resolve();
        if (keyType == null) {
            throw new IllegalStateException("Cannot resolve the key type of " + binding.valueType().getName());
        }
        return keyType;
    }

    private static Instant eventTime(Map<String, String> attributes, SqsMessageMetadata metadata) {
        var published = attributes.get(EVENT_TIME_ATTRIBUTE);
        if (published != null) {
            try {
                return Instant.parse(published);
            } catch (DateTimeParseException e) {
                throw new IllegalStateException(
                        "Attribute %s is not an ISO-8601 instant: %s".formatted(EVENT_TIME_ATTRIBUTE, published), e);
            }
        }
        return metadata.sentTimestamp();
    }
}
