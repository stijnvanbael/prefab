package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.PartitioningKey;

public record WordCount(
        @PartitioningKey
        String word,
        int count
) {
}
