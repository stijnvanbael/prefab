package be.appify.prefab.test.inmemory.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSink;
import be.appify.prefab.streams.tracing.MessageTracing;
import be.appify.prefab.test.inmemory.messaging.InMemoryEventBus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Publishes the outbox records of the engine on the {@link InMemoryEventBus}.
 *
 * <p>The bus encodes the output with the serializer of the platform the event type is registered for, so the output
 * has the bytes a real broker would carry. A topic that a stream names itself becomes an alias of the output type in
 * the {@link EventRegistry}, which lets the bus encode and decode the output on that topic.
 *
 * <p>The bus carries events, so a record without a value, a tombstone, cannot be published. A topology on the bus
 * never receives one, because no application event is a tombstone.
 */
public final class InMemoryBusStreamSink implements StreamSink {
    private final InMemoryEventBus bus;
    private final EventRegistry eventRegistry;

    /**
     * Creates a sink.
     *
     * @param bus the bus to publish on
     * @param eventRegistry registry of the events and their topics
     */
    public InMemoryBusStreamSink(InMemoryEventBus bus, EventRegistry eventRegistry) {
        this.bus = bus;
        this.eventRegistry = eventRegistry;
    }

    @Override
    public void publish(String topic, StreamMessage message) {
        if (message.value() == null) {
            throw new UnsupportedOperationException(
                    "The in-memory bus cannot carry a record without a value (tombstone) to topic '%s'"
                            .formatted(topic));
        }
        var publication = publication(message);
        registerOutput(topic, publication);
        bus.publish(publication, topic, headers(message, publication));
    }

    private Object publication(StreamMessage message) {
        var value = message.value();
        if (value.getClass().getTypeParameters().length == 0) {
            return value;
        }
        var identity = message.headers().get(EventType.HEADER);
        if (identity == null) {
            throw new IllegalArgumentException("Generic stream output is missing " + EventType.HEADER);
        }
        return typedEvent(eventRegistry.concreteTypeFor(identity), value);
    }

    @SuppressWarnings("unchecked")
    private static <T> TypedEvent<T> typedEvent(EventType<T> type, Object value) {
        return new TypedEvent<>(type, (T) type.rawType().cast(value));
    }

    private void registerOutput(String topic, Object publication) {
        if (publication instanceof TypedEvent<?> typed) {
            if (!eventRegistry.topicsForType(typed.type()).contains(topic)) {
                eventRegistry.registerTopicAlias(topic, typed.type(), serializationFor(
                        eventRegistry.topicsForType(typed.type())));
            }
            return;
        }
        var type = publication.getClass();
        if (!eventRegistry.topicsForType(type).contains(topic)) {
            var serialization = eventRegistry.contains(topic)
                    ? eventRegistry.serialization(topic) : serializationFor(eventRegistry.topicsForType(type));
            eventRegistry.registerTopicAlias(topic, type, serialization);
        }
    }

    private Event.Serialization serializationFor(Set<String> topics) {
        var formats = topics.stream().filter(eventRegistry::contains).map(eventRegistry::serialization)
                .distinct().toList();
        if (formats.size() > 1) {
            throw new IllegalStateException("Output type has conflicting serialization formats: " + formats);
        }
        return formats.isEmpty() ? Event.Serialization.JSON : formats.getFirst();
    }

    private static Map<String, String> headers(StreamMessage message, Object publication) {
        var headers = new LinkedHashMap<>(message.headers());
        headers.remove(EventVersions.EVENT_VERSION_HEADER);
        headers.remove(EventVersions.COMPONENT_VERSIONS_HEADER);
        headers.remove(EventType.HEADER);
        headers.remove("type");
        headers.put(MessageTracing.MESSAGE_ID_HEADER, message.messageId());
        headers.put(InMemoryBusStreamSource.EVENT_TIME_HEADER, message.eventTime().toString());
        if (publication instanceof TypedEvent<?> typed) {
            headers.put(EventType.HEADER, typed.type().id());
        }
        return headers;
    }
}
