package be.appify.prefab.streams.kafka;

import java.util.List;

/**
 * A value together with the IDs of the consumed messages it derives from. Used inside operators whose state
 * hides record headers (joins, aggregations).
 *
 * @param value   the wrapped value
 * @param origins the correlation IDs, oldest first
 * @param <V>     the value type
 */
record TracedValue<V>(V value, List<String> origins) {
    TracedValue {
        origins = List.copyOf(origins);
    }
}
