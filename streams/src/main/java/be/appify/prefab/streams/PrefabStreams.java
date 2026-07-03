package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import java.util.Objects;

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

    default <KS, VS extends Keyed<KS>> Store<KS, VS> sharedStore(String name, Class<VS> type) {
        return sharedStore(name, TypeReference.of(type));
    }

    default <KS, VS extends Keyed<KS>> Store<KS, VS> sharedStore(String name, Class<KS> keyType, Class<VS> valueType) {
        Objects.requireNonNull(keyType, "keyType must not be null");
        return sharedStore(name, TypeReference.of(valueType));
    }

    <KS, VS extends Keyed<KS>> Store<KS, VS> sharedStore(String name, TypeReference<VS> type);
}
