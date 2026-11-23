package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.serialization.ConcreteEventSupport;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

/**
 * Encodes events with the Kafka serializers Prefab uses in production, headers included.
 */
public class KafkaMessageCodec implements MessageCodec {
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;
    private final EventRegistry eventRegistry;

    /**
     * Constructs a codec.
     *
     * @param serializer    the serializer used at publish time
     * @param deserializer  the deserializer used by every subscriber
     * @param eventRegistry the registry that knows the registered event types
     */
    public KafkaMessageCodec(
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            EventRegistry eventRegistry
    ) {
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.eventRegistry = eventRegistry;
    }

    @Override
    public boolean supports(Class<?> eventType) {
        return eventRegistry.hasTopicForType(eventType);
    }

    @Override
    public boolean supports(EventType<?> eventType) {
        return !eventRegistry.topicsForType(eventType).isEmpty()
                && ConcreteEventSupport.usesPlatform(eventType, Event.Platform.KAFKA);
    }

    @Override
    public EncodedMessage encode(String topic, Object event) {
        var headers = new RecordHeaders();
        var payload = serializer.serialize(topic, headers, event);
        return new EncodedMessage(topic, toMap(headers), payload, this);
    }

    @Override
    public Object decode(EncodedMessage message) {
        return deserializer.deserialize(message.topic(), toHeaders(message.headers()), message.payload());
    }

    private static Map<String, String> toMap(Headers headers) {
        var map = new HashMap<String, String>();
        headers.forEach(header -> map.put(header.key(), new String(header.value(), StandardCharsets.UTF_8)));
        return map;
    }

    private static Headers toHeaders(Map<String, String> map) {
        var headers = new RecordHeaders();
        map.forEach((name, value) -> headers.add(name, value.getBytes(StandardCharsets.UTF_8)));
        return headers;
    }
}
