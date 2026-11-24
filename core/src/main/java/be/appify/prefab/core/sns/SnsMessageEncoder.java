package be.appify.prefab.core.sns;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.versioning.EventVersions;
import java.util.HashMap;
import java.util.Map;

/**
 * Turns a domain event into the content of an SNS notification: the serialized payload, the subject (the class name of
 * the event) and the message attributes consumers rely on (event and component versions and the generic
 * {@link EventType#HEADER}).
 * <p>
 * The real publisher and the in-memory bus of {@code prefab-test} both use this class, so both produce the same
 * notifications. FIFO group and deduplication ids are transport concerns of the real publisher and not part of it.
 */
public class SnsMessageEncoder {
    private final SnsSerializer serializer;

    /**
     * Constructs an encoder.
     *
     * @param serializer the serializer for the notification payload
     */
    public SnsMessageEncoder(SnsSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Encodes an event for a topic.
     *
     * @param topic the topic name, which selects the serialization format
     * @param event the event, possibly wrapped in a {@link TypedEvent}
     * @return the notification content
     */
    public SnsMessage encode(String topic, Object event) {
        var headers = new HashMap<>(EventVersions.headersFor(event));
        if (event instanceof TypedEvent<?> typed) {
            headers.put(EventType.HEADER, typed.type().id());
        }
        return new SnsMessage(
                serializer.serialize(topic, event),
                TypedEvent.payload(event).getClass().getName(),
                headers);
    }

    /**
     * The content of an SNS notification.
     *
     * @param payload the serialized event
     * @param subject the class name of the event
     * @param headers the message attributes, such as version headers
     */
    public record SnsMessage(String payload, String subject, Map<String, String> headers) {

        /**
         * Constructs a notification content, copying the headers.
         *
         * @param payload the serialized event
         * @param subject the class name of the event
         * @param headers the message attributes
         */
        public SnsMessage {
            headers = Map.copyOf(headers);
        }
    }
}
