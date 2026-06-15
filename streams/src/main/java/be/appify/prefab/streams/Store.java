package be.appify.prefab.streams;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface Store<K, V> {
    Optional<V> get(K key);

    void put(K key, V value);

    default V putOrUpdate(K key, Supplier<V> create, UnaryOperator<V> update) {
        var newValue = get(key).map(update).orElseGet(create);
        put(key, newValue);
        return newValue;
    }

    String name();

    void init(StreamProcessorContext<?, ?> context);
}
