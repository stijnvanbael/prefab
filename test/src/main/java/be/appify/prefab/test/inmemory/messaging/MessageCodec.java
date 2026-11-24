package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.domain.EventType;

/**
 * Serializes events for one messaging platform, so the in-memory bus carries the same bytes a real broker would.
 * <p>
 * The bus asks each codec whether it claims an event type; further platforms plug in by contributing a codec bean.
 */
public interface MessageCodec {

    /**
     * Whether this codec claims events of the given class.
     *
     * @param eventType the event class
     * @return {@code true} when the class is registered for this codec's platform
     */
    boolean supports(Class<?> eventType);

    /**
     * Whether this codec claims the given concrete (generic) event binding.
     *
     * @param eventType the event binding
     * @return {@code true} when the binding is registered for this codec's platform
     */
    boolean supports(EventType<?> eventType);

    /**
     * Serializes an event for a topic.
     *
     * @param topic the destination topic
     * @param event the event, possibly wrapped in a {@code TypedEvent}
     * @return the encoded message
     */
    EncodedMessage encode(String topic, Object event);

    /**
     * Deserializes a message, applying versions and migrations.
     *
     * @param message the message to decode
     * @return the event
     */
    Object decode(EncodedMessage message);
}
