package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.domain.Keyed;

public record WordCount(
        @PartitioningKey
        Word word,
        int count
) implements Keyed<Word> {
    @Override
    public Word key() {
        return word;
    }
}
