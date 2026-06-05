package be.appify.prefab.example.streams;

import be.appify.prefab.core.domain.Key;

public record Word(String value) implements Key<Word> {
    static {
        Key.register(Word.class, Word::new);
    }
}
