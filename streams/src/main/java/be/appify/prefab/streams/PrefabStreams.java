package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;

/**
 * Entry point for the Prefab streams DSL.
 */
public interface PrefabStreams {
    <K extends Key<K>, V extends Keyed<K>> PrefabStream<K, V> from(Class<V> type);

    <K extends Key<K>, M extends Keyed<K>> PrefabStream<K, M> merge(PrefabStream<K, ? extends M> left, PrefabStream<K, ? extends M> right);

    <KS extends Key<KS>, VS extends Keyed<KS>> Store<KS, VS> createStore(Class<VS> type);
}
