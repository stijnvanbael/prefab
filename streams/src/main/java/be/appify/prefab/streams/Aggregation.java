package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;

import java.util.ArrayList;
import java.util.List;

public record Aggregation<K extends Key<K>, V>(
        K key,
        List<V> values
) implements Keyed<K> {
    public static <K extends Key<K>, V> Aggregation<K, V> create(K key, V value) {
        return new Aggregation<>(key, List.of(value));
    }

    public Aggregation<K, V> append(V value) {
        var newValues = new ArrayList<>(values);
        newValues.add(value);
        return new Aggregation<>(key, newValues);
    }
}
