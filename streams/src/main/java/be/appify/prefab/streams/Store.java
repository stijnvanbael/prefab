package be.appify.prefab.streams;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface Store<T> {
    Optional<T> get(String key);

    void put(String key, T value);

    default T putOrUpdate(String key, Supplier<T> create, UnaryOperator<T> update) {
        var newValue = get(key).map(update).orElseGet(create);
        put(key, newValue);
        return newValue;
    }

    String name();

    void init(StreamProcessorContext<?> context);
}
