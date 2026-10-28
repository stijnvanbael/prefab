package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

/**
 * Entry point for the Prefab streams DSL.
 */
public interface PrefabStreams {
    <K, V extends Keyed<K>> PrefabStream<K, V> from(Class<V> type);

    <K, M extends Keyed<K>> PrefabStream<K, M> merge(PrefabStream<K, ? extends M> left, PrefabStream<K, ? extends M> right);

    default <KS, VS extends Keyed<KS>> Store<KS, VS> createStore(Class<VS> type) {
        return createStore(TypeReference.of(type));
    }

    <KS, VS extends Keyed<KS>> Store<KS, VS> createStore(TypeReference<VS> type);

    /**
     * Declares a global store that is populated from the topic registered for {@code type}.
     *
     * <p>Every application instance materialises all records of the topic, keyed by {@link Keyed#key()}. A record
     * with a {@code null} value removes the key. Declaring the same type more than once returns the same store.
     * Bind the store to processors with {@link PrefabStream#process(StreamProcessor, ReadableStore[])}.
     *
     * @param type
     *         the value type of the source topic
     * @return the read-only global store
     */
    <KS, VS extends Keyed<KS>> GlobalStore<KS, VS> globalStore(Class<VS> type);
}
