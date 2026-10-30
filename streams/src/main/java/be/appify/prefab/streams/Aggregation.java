package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.tracing.MessageTracing;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * State of an aggregation: the values folded for a key and, when message tracing is enabled, the IDs of the
 * messages they originate from.
 *
 * @param key            the aggregation key
 * @param values         the folded values
 * @param correlationIds the IDs of the messages the values derive from, oldest first; omitted from the
 *                       serialised form when empty so that state written without tracing is unchanged
 * @param <K>            the key type
 * @param <V>            the value type
 */
public record Aggregation<K, V>(
        K key,
        List<V> values,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> correlationIds
) implements Keyed<K> {
    /** Normalises absent correlation IDs, such as in state written before tracing existed, to an empty list. */
    public Aggregation {
        correlationIds = correlationIds == null ? List.of() : List.copyOf(correlationIds);
    }

    /**
     * Creates an aggregation without correlation IDs.
     *
     * @param key    the aggregation key
     * @param values the folded values
     */
    public Aggregation(K key, List<V> values) {
        this(key, values, List.of());
    }

    public static <K, V> Aggregation<K, V> create(K key, V value) {
        return new Aggregation<>(key, List.of(value));
    }

    public Aggregation<K, V> append(V value) {
        var newValues = new ArrayList<>(values);
        newValues.add(value);
        return new Aggregation<>(key, newValues, correlationIds);
    }

    /**
     * Returns a copy that also derives from the given messages.
     *
     * @param ids the IDs to add
     * @param max the maximum number of IDs to keep; the oldest are evicted first
     * @return the aggregation with merged correlation IDs
     */
    public Aggregation<K, V> withCorrelation(List<String> ids, int max) {
        return new Aggregation<>(key, values, MessageTracing.union(correlationIds, ids, max));
    }
}

