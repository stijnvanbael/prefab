package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

import java.time.Instant;
import java.util.Map;

public record StreamRecord<K, V extends Keyed<K>>(
        K key,
        V value,
        Instant timestamp,
        Map<String, String> headers
) {
}