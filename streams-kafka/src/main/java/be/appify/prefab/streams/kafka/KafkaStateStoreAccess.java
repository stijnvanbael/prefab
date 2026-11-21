package be.appify.prefab.streams.kafka;

import org.apache.kafka.streams.state.KeyValueStore;

/** Lets a test session give a store direct access to its state, so it can be read outside of a processor. */
public interface KafkaStateStoreAccess {
    /** Name of the underlying state store. */
    String storeName();

    /**
     * Binds the state store to read from when no processor has initialised this store.
     *
     * @param store the state store
     */
    void attach(KeyValueStore<?, ?> store);
}
