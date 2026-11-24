package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubMessageEncoder;
import be.appify.prefab.core.serialization.ConcreteEventSupport;
import com.google.protobuf.ByteString;

/**
 * Encodes events like the Pub/Sub publisher does: the same payload and message attributes, produced by the same
 * {@link PubSubMessageEncoder}.
 */
public class PubSubMessageCodec implements MessageCodec {
    private final PubSubMessageEncoder encoder;
    private final PubSubDeserializer deserializer;
    private final EventRegistry eventRegistry;

    /**
     * Constructs a codec.
     *
     * @param encoder       the encoder shared with the real Pub/Sub publisher
     * @param deserializer  the deserializer shared with the real Pub/Sub subscriber
     * @param eventRegistry the registry that knows the registered event types
     */
    public PubSubMessageCodec(
            PubSubMessageEncoder encoder,
            PubSubDeserializer deserializer,
            EventRegistry eventRegistry
    ) {
        this.encoder = encoder;
        this.deserializer = deserializer;
        this.eventRegistry = eventRegistry;
    }

    @Override
    public boolean supports(Class<?> eventType) {
        return eventRegistry.hasTopicForType(eventType)
                && ConcreteEventSupport.usesPlatform(eventType, Event.Platform.PUB_SUB);
    }

    @Override
    public boolean supports(EventType<?> eventType) {
        return !eventRegistry.topicsForType(eventType).isEmpty()
                && ConcreteEventSupport.usesPlatform(eventType, Event.Platform.PUB_SUB);
    }

    @Override
    public EncodedMessage encode(String topic, Object event) {
        var message = encoder.encode(topic, event);
        return new EncodedMessage(topic, message.getAttributesMap(), message.getData().toByteArray(), this);
    }

    @Override
    public Object decode(EncodedMessage message) {
        var data = ByteString.copyFrom(message.payload());
        var identity = message.header(EventType.HEADER);
        if (identity.isPresent()) {
            return deserializer.deserialize(message.topic(), data,
                    eventRegistry.concreteTypeFor(message.topic(), identity.get()), message.headers());
        }
        var typeName = message.header(PubSubMessageEncoder.TYPE_ATTRIBUTE)
                .orElseThrow(() -> new IllegalStateException("Pub/Sub message without type attribute"));
        var type = eventRegistry.typeByClassName(typeName).orElseThrow(() -> new UnknownEventTypeException(typeName));
        return deserializer.deserialize(message.topic(), data, type, message.headers());
    }
}
