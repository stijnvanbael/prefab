package be.appify.prefab.streams;

import java.util.Optional;

/**
 * Read access to a state store that can be bound to a {@link StreamProcessor}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface ReadableStore<K, V> {
    /**
     * Looks up the value stored for a key.
     *
     * @param key the key to look up
     * @return the stored value, or empty when the key is absent
     */
    Optional<V> get(K key);

    /**
     * Returns the unique store name within the topology.
     *
     * @return the store name
     */
    String name();

    /**
     * Binds the store to the processor context. Called by the framework when a processor is initialised.
     *
     * @param context the processor context
     */
    void init(StreamProcessorContext<?, ?> context);
}
