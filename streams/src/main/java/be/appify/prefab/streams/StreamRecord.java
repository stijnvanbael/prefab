package be.appify.prefab.streams;

import java.util.Map;

public record StreamRecord<V>(String key, V value, Map<String, Object> headers) {
}
