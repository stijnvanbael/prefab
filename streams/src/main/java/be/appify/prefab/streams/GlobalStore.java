package be.appify.prefab.streams;

/**
 * A read-only state store that is fully replicated to every application instance.
 *
 * <p>Unlike a partitioned {@link Store}, every instance holds all records of the source topic, so a processor can
 * look up any key regardless of which partition the record being processed came from. The store is populated
 * exclusively from its source topic; processors cannot write to it.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface GlobalStore<K, V> extends ReadableStore<K, V>, TopologyElement {
}
