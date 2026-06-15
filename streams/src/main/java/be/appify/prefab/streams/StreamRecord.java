package be.appify.prefab.streams;

import java.time.Instant;
import java.util.Map;

public record StreamRecord<K, V>(
        K key,
        V value,
        Instant timestamp,
        Map<String, String> headers
) {
}