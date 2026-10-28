package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

/**
 * Keeps a global store up to date from the records of its source topic.
 *
 * <p>An updater only consumes: it cannot forward records, has no processor context and cannot bind other stores,
 * because the store it receives is the only thing a global store may be written from. It is the single writer of the
 * store; all other processors see a read-only {@link GlobalStore}.
 *
 * <p>Records with a {@code null} value (tombstones) are not passed to the updater.
 *
 * @param <KI> the key type of the source records
 * @param <VI> the value type of the source records
 * @param <K> the key type of the store
 * @param <V> the value type of the store
 */
@FunctionalInterface
public interface GlobalStoreUpdater<KI, VI extends Keyed<KI>, K, V extends Keyed<K>> {
    /**
     * Applies a source record to the store.
     *
     * @param streamRecord
     *         the source record
     * @param store
     *         the writable view on the global store
     */
    void update(StreamRecord<KI, VI> streamRecord, Store<K, V> store);
}
