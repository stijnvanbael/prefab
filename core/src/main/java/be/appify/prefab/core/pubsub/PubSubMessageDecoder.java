package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the event a subscription is interested in from a received Pub/Sub message, using the attributes that
 * {@link PubSubMessageEncoder} stamped on it.
 * <p>
 * The real subscriber and the in-memory bus of {@code prefab-test} both use this class.
 */
public class PubSubMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(PubSubMessageDecoder.class);

    private final PubSubDeserializer deserializer;
    private final EventRegistry eventRegistry;

    /**
     * Constructs a decoder.
     *
     * @param deserializer  the deserializer for the message payload
     * @param eventRegistry the registry that knows the event types and concrete bindings
     */
    public PubSubMessageDecoder(PubSubDeserializer deserializer, EventRegistry eventRegistry) {
        this.deserializer = deserializer;
        this.eventRegistry = eventRegistry;
    }

    /**
     * Decodes a message for a subscription.
     *
     * @param request the subscription that receives the event
     * @param message the received message
     * @param <T>     the event type the subscription expects
     * @return the event, or empty when the message belongs to another binding, has an unknown event type, or is
     * not an instance of the type the subscription expects
     */
    public <T> Optional<T> decode(SubscriptionRequest<T> request, PubsubMessage message) {
        if (request.eventType().isPresent()) {
            return decodeConcrete(request, message);
        }
        if (message.containsAttributes(PubSubMessageEncoder.TYPE_ATTRIBUTE)) {
            return decodeTyped(request, message);
        }
        return Optional.of(deserialize(request, message));
    }

    private <T> Optional<T> decodeConcrete(SubscriptionRequest<T> request, PubsubMessage message) {
        var type = request.eventType().orElseThrow();
        var identity = message.getAttributesMap().get(EventType.HEADER);
        if (identity == null) {
            throw new IllegalArgumentException("Generic event is missing " + EventType.HEADER);
        }
        var registered = eventRegistry.concreteTypeFor(request.topic(), identity);
        if (!registered.equals(type)) {
            log.debug("Skipping concrete event [{}] for Pub/Sub binding [{}]", registered, type);
            return Optional.empty();
        }
        return Optional.of(deserializer.deserialize(
                request.topic(), message.getData(), type, message.getAttributesMap()));
    }

    private <T> Optional<T> decodeTyped(SubscriptionRequest<T> request, PubsubMessage message) {
        var typeName = message.getAttributesOrThrow(PubSubMessageEncoder.TYPE_ATTRIBUTE);
        var consumedType = eventRegistry.typeByClassName(typeName);
        if (consumedType.isEmpty()) {
            log.warn("Ignoring Pub/Sub message with unknown event type [{}] on topic [{}]",
                    typeName, request.topic());
            return Optional.empty();
        }
        return request.type().isAssignableFrom(consumedType.get())
                ? Optional.of(deserialize(request, message))
                : Optional.empty();
    }

    private <T> T deserialize(SubscriptionRequest<T> request, PubsubMessage message) {
        return deserializer.deserialize(request.topic(), message.getData(), request.type(), message.getAttributesMap());
    }
}
