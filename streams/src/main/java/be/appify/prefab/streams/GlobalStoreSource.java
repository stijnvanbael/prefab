package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.domain.EventType;
import java.util.Objects;

/**
 * First step of declaring a global store that is maintained by a {@link GlobalStoreUpdater}.
 *
 * @param <KS> the key type of the source records
 * @param <S> the value type of the source records
 * @see PrefabStreams#globalStoreFrom(Class)
 */
public final class GlobalStoreSource<KS, S extends Keyed<KS>> {
    private final PrefabStreams streams;
    private final TypeReference<S> sourceType;

    GlobalStoreSource(PrefabStreams streams, Class<S> sourceType) {
        this.streams = Objects.requireNonNull(streams, "streams must not be null");
        this.sourceType = TypeReference.of(Objects.requireNonNull(sourceType, "sourceType must not be null"));
    }

    GlobalStoreSource(PrefabStreams streams, EventType<S> sourceType) {
        this.streams = Objects.requireNonNull(streams);
        this.sourceType = TypeReference.of(sourceType);
    }

    /**
     * Sets the updater that writes the store from the source records.
     *
     * @param updater
     *         the single writer of the store
     * @param <K>
     *         the key type of the store
     * @param <V>
     *         the value type of the store
     * @return the next declaration step
     */
    public <K, V extends Keyed<K>> GlobalStoreDeclaration<KS, S, K, V> updatedBy(
            GlobalStoreUpdater<KS, S, K, V> updater
    ) {
        return new GlobalStoreDeclaration<>(streams, sourceType, Objects.requireNonNull(updater, "updater must not be null"));
    }
}
