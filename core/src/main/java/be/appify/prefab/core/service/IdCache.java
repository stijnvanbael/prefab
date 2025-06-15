package be.appify.prefab.core.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IdCache {
    public static final IdCache INSTANCE = new IdCache();

    private final ThreadLocal<Map<Class<?>, Map<Object, String>>> localCache = new ThreadLocal<>();

    public String getId(Object aggregate) {
        return cache().computeIfAbsent(aggregate.getClass(), _ -> new HashMap<>())
                .computeIfAbsent(aggregate, _ -> generateId());
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    public void flush(Object aggregate) { // TODO: flush on save
        cache().computeIfAbsent(aggregate.getClass(), _ -> new HashMap<>())
                .remove(aggregate);
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
}
