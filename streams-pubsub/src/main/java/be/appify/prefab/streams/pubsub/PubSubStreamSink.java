package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.streams.StreamKeyEncoding;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSink;
import be.appify.prefab.streams.tracing.MessageTracing;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

final class PubSubStreamSink implements StreamSink, AutoCloseable {
    private final PubSubUtil util;
    private final PubSubSerializer serializer;
    private final EventRegistry registry;
    private final PublisherFactory factory;
    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    PubSubStreamSink(PubSubUtil util, PubSubSerializer serializer, EventRegistry registry, PublisherFactory factory) {
        this.util = util;
        this.serializer = serializer;
        this.registry = registry;
        this.factory = factory;
    }

    @Override
    public void publish(String topic, StreamMessage message) {
        var publisher = publishers.computeIfAbsent(topic, name -> factory.createPublisher(util.ensureTopicExists(name)));
        var orderingKey = encodeKey(message.key());
        var payload = publication(topic, message);
        registerOutput(topic, payload);
        var attributes = attributes(message, payload);
        var bytes = payload == null ? ByteString.EMPTY : ByteString.copyFrom(serializer.serialize(topic, payload));
        var output = PubsubMessage.newBuilder().setData(bytes).setOrderingKey(orderingKey)
                .putAllAttributes(attributes).build();
        try {
            publisher.publish(output).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resume(publisher, orderingKey, e);
            throw new IllegalStateException("Interrupted publishing stream output to " + topic, e);
        } catch (ExecutionException | RuntimeException e) {
            resume(publisher, orderingKey, e);
            throw new IllegalStateException("Failed to publish stream output to " + topic, e);
        }
    }

    private Object publication(String topic, StreamMessage message) {
        var value = message.value();
        if (value == null || value.getClass().getTypeParameters().length == 0) {
            return value;
        }
        var identity = message.headers().get(EventType.HEADER);
        if (identity == null) {
            throw new IllegalArgumentException("Generic stream output is missing " + EventType.HEADER);
        }
        return typedEvent(registry.concreteTypeFor(identity), value);
    }

    private void registerOutput(String topic, Object publication) {
        if (publication == null) {
            return;
        }
        if (publication instanceof TypedEvent<?> typed) {
            if (!registry.topicsForType(typed.type()).contains(topic)) {
                var topics = registry.topicsForType(typed.type());
                var serialization = serializationFor(topics);
                registry.registerTopicAlias(topic, typed.type(), serialization);
            }
        } else {
            var valueType = publication.getClass();
            var serialization = registry.contains(topic)
                    ? registry.serialization(topic) : serializationFor(registry.topicsForType(valueType));
            registry.registerTopicAlias(topic, valueType, serialization);
        }
    }

    private Event.Serialization serializationFor(java.util.Set<String> topics) {
        var formats = topics.stream().filter(registry::contains).map(registry::serialization).distinct().toList();
        if (formats.size() > 1) {
            throw new IllegalStateException("Output type has conflicting serialization formats: " + formats);
        }
        return formats.isEmpty() ? Event.Serialization.JSON : formats.getFirst();
    }

    @SuppressWarnings("unchecked")
    private static <T> TypedEvent<T> typedEvent(EventType<T> type, Object value) {
        return new TypedEvent<>(type, (T) type.rawType().cast(value));
    }

    private Map<String, String> attributes(StreamMessage message, Object publication) {
        var headers = new LinkedHashMap<>(message.headers());
        headers.remove(EventVersions.EVENT_VERSION_HEADER);
        headers.remove(EventVersions.COMPONENT_VERSIONS_HEADER);
        headers.remove(EventType.HEADER);
        headers.remove("type");
        headers.put(MessageTracing.MESSAGE_ID_HEADER, message.messageId());
        headers.put(PubSubStreamSource.EVENT_TIME, message.eventTime().toString());
        if (publication == null) {
            if (message.headers().containsKey(EventType.HEADER)) {
                headers.put(EventType.HEADER, message.headers().get(EventType.HEADER));
            }
            if (message.headers().containsKey("type")) {
                headers.put("type", message.headers().get("type"));
            }
            headers.put(PubSubStreamSource.TOMBSTONE, "true");
        } else {
            headers.remove(PubSubStreamSource.TOMBSTONE);
            headers.put("type", TypedEvent.payload(publication).getClass().getName());
            headers.putAll(EventVersions.headersFor(publication));
            if (publication instanceof TypedEvent<?> typed) {
                headers.put(EventType.HEADER, typed.type().id());
            }
        }
        var ids = MessageTracing.decode(headers.get(MessageTracing.CORRELATION_IDS_HEADER));
        while (!ids.isEmpty() && MessageTracing.encode(ids).getBytes(StandardCharsets.UTF_8).length > 1024) {
            ids = ids.subList(1, ids.size());
        }
        if (ids.isEmpty()) {
            headers.remove(MessageTracing.CORRELATION_IDS_HEADER);
        } else {
            headers.put(MessageTracing.CORRELATION_IDS_HEADER, MessageTracing.encode(ids));
        }
        return headers;
    }

    private static String encodeKey(Object key) {
        if (key == null) {
            return "";
        }
        if (key instanceof String string) {
            return string;
        }
        return encodeTypedKey(key);
    }

    @SuppressWarnings("unchecked")
    private static <K> String encodeTypedKey(K key) {
        var encoder = new StreamKeyEncoding<>((Class<K>) key.getClass());
        return new String(encoder.encode(key), StandardCharsets.UTF_8);
    }

    private static void resume(Publisher publisher, String key, Exception cause) {
        if (!key.isEmpty()) {
            try {
                publisher.resumePublish(key);
            } catch (RuntimeException failure) {
                cause.addSuppressed(failure);
            }
        }
    }

    @Override
    public void close() {
        publishers.values().forEach(Publisher::shutdown);
    }
}
