package be.appify.prefab.core.util;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;

/**
 * @deprecated Use {@link EventRegistry} directly. {@code SerializationRegistry} is a thin backwards-compatibility
 * wrapper that delegates all calls to an {@link EventRegistry}. Inject {@link EventRegistry} instead.
 */
@Deprecated(since = "0.9", forRemoval = true)
public class SerializationRegistry {

    private final EventRegistry delegate;

    /**
     * Constructs a standalone SerializationRegistry backed by a new EventRegistry.
     * Prefer injecting {@link EventRegistry} directly in new code.
     */
    public SerializationRegistry() {
        this.delegate = new EventRegistry();
    }

    /**
     * Constructs a SerializationRegistry that delegates to the given EventRegistry.
     *
     * @param delegate the EventRegistry to delegate to
     */
    public SerializationRegistry(EventRegistry delegate) {
        this.delegate = delegate;
    }

    /**
     * @deprecated Use {@link EventRegistry#register(String, Event.Serialization)} instead.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public void register(String topic, Event.Serialization serialization) {
        delegate.register(topic, serialization);
    }

    /**
     * @deprecated Use {@link EventRegistry#serialization(String)} instead.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public Event.Serialization get(String topic) {
        return delegate.serialization(topic);
    }

    /**
     * @deprecated Use {@link EventRegistry#contains(String)} instead.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public boolean contains(String topic) {
        return delegate.contains(topic);
    }

    /**
     * @deprecated Use {@link EventRegistry#hasSerialization(Event.Serialization)} instead.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public boolean hasSerialization(Event.Serialization serialization) {
        return delegate.hasSerialization(serialization);
    }
}
