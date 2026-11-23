package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import be.appify.prefab.streams.StreamKeyEncoding;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSource;
import be.appify.prefab.streams.tracing.MessageTracing;
import com.google.pubsub.v1.PubsubMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.ResolvableType;

final class PubSubStreamSource implements StreamSource {
    static final String EVENT_TIME = "prefab-event-time";
    static final String TOMBSTONE = "prefab-tombstone";
    private final String application;
    private final PubSubUtil util;
    private final PubSubDeserializer deserializer;
    private final EventRegistry registry;

    PubSubStreamSource(String application, PubSubUtil util, PubSubDeserializer deserializer, EventRegistry registry) {
        this.application = application;
        this.util = util;
        this.deserializer = deserializer;
        this.registry = registry;
    }

    @Override
    public boolean ordered(String topic) {
        return true;
    }

    @Override
    public Subscription subscribe(String topic, MessageHandler handler) {
        throw new IllegalArgumentException("Pub/Sub stream sources require a named event binding");
    }

    @Override
    public Subscription subscribe(SourceBinding binding, MessageHandler handler) {
        var request = SubscriptionRequest.raw(binding.topic(), application + ".streams." + binding.name(),
                message -> consume(binding, handler, message));
        var subscriber = util.subscribeWithHandle(request);
        return subscriber::stopAsync;
    }

    private void consume(SourceBinding binding, MessageHandler handler, PubsubMessage message) {
        var concrete = concreteType(binding, message);
        if (binding.eventTypeId() != null && !binding.eventTypeId().equals(concrete.id())) {
            return;
        }
        var typeName = message.getAttributesMap().get("type");
        var valueType = binding.valueType();
        if (typeName != null) {
            var actual = registry.typeByClassName(typeName).orElseThrow(() ->
                    new IllegalArgumentException("Unregistered event type " + typeName + " on " + binding.topic()));
            if (!binding.valueType().isAssignableFrom(actual)
                    || (concrete == null && registry.registeredTypesForTopic(binding.topic()).stream()
                            .noneMatch(type -> type.isAssignableFrom(actual)))) {
                return;
            }
            valueType = actual;
        }
        var tombstone = "true".equals(message.getAttributesMap().get(TOMBSTONE));
        if (tombstone && !message.getData().isEmpty()) {
            throw new IllegalArgumentException("A Pub/Sub stream tombstone must have an empty body");
        }
        var value = tombstone ? null : deserialize(binding, concrete, valueType, message);
        if (tombstone && message.getOrderingKey().isEmpty()) {
            throw new IllegalArgumentException("A Pub/Sub stream tombstone requires an ordering key");
        }
        var key = message.getOrderingKey().isEmpty()
                ? ((Keyed<?>) value).key() : decodeKey(binding, concrete, message.getOrderingKey());
        var id = message.getAttributesMap().getOrDefault(MessageTracing.MESSAGE_ID_HEADER, message.getMessageId());
        if (id.isBlank()) {
            throw new IllegalArgumentException("A Pub/Sub stream message must have a message ID");
        }
        var timeHeader = message.getAttributesMap().get(EVENT_TIME);
        if (timeHeader == null && !message.hasPublishTime()) {
            throw new IllegalArgumentException("A Pub/Sub stream message must have a publish time or event time");
        }
        var timestamp = timeHeader == null
                ? Instant.ofEpochSecond(message.getPublishTime().getSeconds(), message.getPublishTime().getNanos())
                : Instant.parse(timeHeader);
        var acknowledged = new AtomicBoolean();
        handler.onMessage(new StreamMessage(id, key, value, message.getAttributesMap(), timestamp),
                () -> acknowledged.set(true));
        if (!acknowledged.get()) {
            throw new IllegalStateException("Stream engine did not acknowledge " + binding.name());
        }
    }

    private EventType<?> concreteType(SourceBinding binding, PubsubMessage message) {
        if (binding.eventTypeId() == null) {
            return null;
        }
        var identity = message.getAttributesMap().get(EventType.HEADER);
        if (identity == null) {
            throw new IllegalArgumentException("Generic event is missing " + EventType.HEADER);
        }
        return registry.concreteTypeFor(binding.topic(), identity);
    }

    private Object deserialize(SourceBinding binding, EventType<?> concrete, Class<?> valueType, PubsubMessage message) {
        return concrete == null
                ? deserializer.deserialize(binding.topic(), message.getData(), valueType, message.getAttributesMap())
                : deserializer.deserialize(binding.topic(), message.getData(), concrete, message.getAttributesMap());
    }

    private Object decodeKey(SourceBinding binding, EventType<?> concrete, String orderingKey) {
        var sourceType = concrete == null
                ? ResolvableType.forClass(binding.valueType()) : ResolvableType.forType(concrete.type());
        var keyType = sourceType.as(Keyed.class).getGeneric(0).resolve();
        if (keyType == null) {
            throw new IllegalStateException("Cannot resolve source key type for " + binding.valueType().getName());
        }
        if (keyType == String.class) {
            return orderingKey;
        }
        return new StreamKeyEncoding<>(keyType).decode(orderingKey.getBytes(StandardCharsets.UTF_8));
    }
}
