package be.appify.prefab.streams;

import java.util.Optional;

public interface Store<T> {
    Optional<T> get(String key);

    void put(String key, T value);

    String name();
}
