package be.appify.prefab.core.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IdCache {
    public static final IdCache INSTANCE = new IdCache();

    private final ThreadLocal<Map<Class<?>, Map<Object, String>>> localCache = new ThreadLocal<>();

    public String getId(Object aggregate) {
        return cacheForClass(aggregate.getClass())
                .computeIfAbsent(aggregate, ignored -> generateId());
    }

    private Map<Object, String> cacheForClass(Class<?> type) {
        return cache().computeIfAbsent(type, ignored -> new HashMap<>());
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    public void clear() {
        cache().clear();
    }

    private Map<Class<?>, Map<Object, String>> cache() {
        var cache = localCache.get();
        if (cache == null) {
            cache = new HashMap<>();
            localCache.set(cache);
        }
        return cache;
    }

    public void put(Object aggregate, String id) {
        cacheForClass(aggregate.getClass()).put(aggregate, id);
    }
}
