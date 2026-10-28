package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import java.util.function.Function;

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
     * @param type
     *         the value type of both the source topic and the store
     * @return the read-only global store
     * @see #globalStore(String, Class, Class, Function)
     */
    default <KS, VS extends Keyed<KS>> GlobalStore<KS, VS> globalStore(Class<VS> type) {
        return globalStore(defaultGlobalStoreName(type), type, type, Function.identity());
    }

    /**
     * Declares a global store with an explicit name, populated from the topic registered for {@code type}.
     *
     * @param name
     *         the unique store name
     * @param type
     *         the value type of both the source topic and the store
     * @return the read-only global store
     * @see #globalStore(String, Class, Class, Function)
     */
    default <KS, VS extends Keyed<KS>> GlobalStore<KS, VS> globalStore(String name, Class<VS> type) {
        return globalStore(name, type, type, Function.identity());
    }

    /**
     * Declares a global store whose content differs from the records of its source topic.
     *
     * @see #globalStore(String, Class, Class, Function)
     */
    default <KS, S extends Keyed<KS>, VS extends Keyed<KS>> GlobalStore<KS, VS> globalStore(
            Class<S> sourceType,
            Class<VS> storeType,
            Function<S, VS> mapper
    ) {
        return globalStore(defaultGlobalStoreName(storeType), sourceType, storeType, mapper);
    }

    /**
     * Declares a global store, a read-only store that is fed from the topic registered for {@code sourceType} and
     * fully replicated to every application instance.
     *
     * <p>Records are keyed by their record key. A record with a {@code null} value, or one that the mapper maps to
     * {@code null}, removes the key. Kafka allows only one global store per source topic, so every source type can
     * back at most one global store; declaring a second store on the same source type fails with an
     * {@link IllegalArgumentException}. Declaring the same name again with the same source and store type returns
     * the existing store. Processors read it by returning it from {@link StreamProcessor#globalStores()}.
     *
     * @param name
     *         the unique store name
     * @param sourceType
     *         the value type of the source topic
     * @param storeType
     *         the value type kept in the store
     * @param mapper
     *         converts a source record to the stored value
     * @return the read-only global store
     */
    <KS, S extends Keyed<KS>, VS extends Keyed<KS>> GlobalStore<KS, VS> globalStore(
            String name,
            Class<S> sourceType,
            Class<VS> storeType,
            Function<S, VS> mapper
    );

    /**
     * Starts the declaration of a global store that is maintained by a {@link GlobalStoreUpdater}, for stores whose
     * content is computed from the source records rather than mapped one-to-one.
     *
     * <pre>{@code
     * var counters = streams.globalStoreFrom(LifecycleEvent.class)
     *         .updatedBy(new SnapshotCounterUpdater())
     *         .as(SnapshotCounter.class);
     * }</pre>
     *
     * @param sourceType
     *         the value type of the source topic
     * @return the first declaration step
     */
    default <KS, S extends Keyed<KS>> GlobalStoreSource<KS, S> globalStoreFrom(Class<S> sourceType) {
        return new GlobalStoreSource<>(this, sourceType);
    }

    /**
     * Declares a global store maintained by an updater. Prefer {@link #globalStoreFrom(Class)}.
     *
     * <p>Kafka allows one global store per source topic and one writer per store, so declaring a name or a source
     * type that is already in use fails with an {@link IllegalArgumentException}.
     *
     * @param name
     *         the unique store name
     * @param sourceType
     *         the value type of the source topic
     * @param storeType
     *         the value type kept in the store
     * @param updater
     *         the single writer of the store
     * @return the read-only global store
     */
    <KS, S extends Keyed<KS>, K, V extends Keyed<K>> GlobalStore<K, V> declareGlobalStore(
            String name,
            Class<S> sourceType,
            Class<V> storeType,
            GlobalStoreUpdater<KS, S, K, V> updater
    );
    /**
     * Returns the default global store name for a store type.
     *
     * @param storeType
     *         the value type kept in the store
     * @return the default store name
     */
    static String defaultGlobalStoreName(Class<?> storeType) {
        return TypeReference.of(storeType).name();
    }
}