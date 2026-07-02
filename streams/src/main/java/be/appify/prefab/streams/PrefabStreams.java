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
}
