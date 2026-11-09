package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.domain.EventType;

/**
 * Final step of declaring a global store: choose the stored type, and optionally the store name.
 *
 * @param <KS> the key type of the source records
 * @param <S> the value type of the source records
 * @param <K> the key type of the store
 * @param <V> the value type of the store
 * @see PrefabStreams#globalStoreFrom(Class)
 */
public final class GlobalStoreDeclaration<KS, S extends Keyed<KS>, K, V extends Keyed<K>> {
    private final PrefabStreams streams;
    private final TypeReference<S> sourceType;
    private final GlobalStoreUpdater<KS, S, K, V> updater;

    GlobalStoreDeclaration(PrefabStreams streams, TypeReference<S> sourceType, GlobalStoreUpdater<KS, S, K, V> updater) {
        this.streams = streams;
        this.sourceType = sourceType;
        this.updater = updater;
    }

    /**
     * Declares the global store under the default name of the store type.
     *
     * @param storeType
     *         the value type kept in the store
     * @return the read-only global store
     */
    public GlobalStore<K, V> as(Class<V> storeType) {
        return as(PrefabStreams.defaultGlobalStoreName(storeType), storeType);
    }

    /**
     * Declares the global store under an explicit name.
     *
     * @param name
     *         the unique store name
     * @param storeType
     *         the value type kept in the store
     * @return the read-only global store
     */
    public GlobalStore<K, V> as(String name, Class<V> storeType) {
        if (sourceType.type() instanceof java.lang.reflect.ParameterizedType) {
            return streams.declareGlobalStore(name, EventType.<S>from(sourceType.type()), TypeReference.of(storeType), updater);
        }
        return streams.declareGlobalStore(name, sourceType.rawType(), storeType, updater);
    }

    /** @param storeType full stored value binding @return replicated store */
    public GlobalStore<K, V> as(EventType<V> storeType) {
        return as(TypeReference.of(storeType).name(), storeType);
    }

    /** @param name explicit store name @param storeType full stored binding @return replicated store */
    public GlobalStore<K, V> as(String name, EventType<V> storeType) {
        return streams.declareGlobalStore(name, EventType.<S>from(sourceType.type()), TypeReference.of(storeType), updater);
    }
}
