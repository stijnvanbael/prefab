package be.appify.prefab.example.streams;

import be.appify.prefab.core.domain.Key;

public record WordId(String value) implements Key<WordId> {
    static {
        Key.register(WordId.class, WordId::new);
    }
}
