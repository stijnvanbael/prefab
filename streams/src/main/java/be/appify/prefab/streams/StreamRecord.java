package be.appify.prefab.streams;

import java.time.Instant;
import java.util.Map;

public record StreamRecord<V>(
        String key,
        V value,
        Instant timestamp,
        Map<String, String> headers
) {
}
