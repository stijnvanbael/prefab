package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.versioning.EventVersions;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Optional;
import java.util.function.Function;

/**
 * Turns a domain event into the Pub/Sub message that is published: the serialized payload plus the attributes
 * consumers rely on (event class name, event and component versions, the generic {@link EventType#HEADER}) and the
 * ordering key.
 * <p>
 * The real publisher and the in-memory bus of {@code prefab-test} both use this class, so both produce the same
 * messages.
 */
public class PubSubMessageEncoder {
    /** Attribute holding the class name of the published event. */
    public static final String TYPE_ATTRIBUTE = "type";

    private final PubSubSerializer serializer;
    private final Function<Object, Optional<String>> orderingKeys;

    /**
     * Constructs an encoder.
     *
     * @param serializer   the serializer for the message payload
     * @param orderingKeys resolves the ordering key of an event, empty when the event has none
     */
    public PubSubMessageEncoder(PubSubSerializer serializer, Function<Object, Optional<String>> orderingKeys) {
        this.serializer = serializer;
        this.orderingKeys = orderingKeys;
    }

    /**
     * Encodes an event for a topic.
     *
     * @param topic the simple (not project-qualified) topic name, which selects the serialization format
     * @param event the event, possibly wrapped in a {@link TypedEvent}
     * @return the message to publish
     */
    public PubsubMessage encode(String topic, Object event) {
        var message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(serializer.serialize(topic, event)))
                .putAttributes(TYPE_ATTRIBUTE, TypedEvent.payload(event).getClass().getName())
                .putAllAttributes(EventVersions.headersFor(event));
        if (event instanceof TypedEvent<?> typed) {
            message.putAttributes(EventType.HEADER, typed.type().id());
        }
        orderingKeys.apply(event).ifPresent(message::setOrderingKey);
        return message.build();
    }
}
