package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.engine.StreamTopicResolver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PubSubStreamTopicResolver implements StreamTopicResolver {
    private final EventRegistry registry;
    private final Map<Class<?>, String> classes = new ConcurrentHashMap<>();
    private final Map<EventType<?>, String> bindings = new ConcurrentHashMap<>();

    PubSubStreamTopicResolver(EventRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String topicFor(Class<?> type) {
        var registered = classes.get(type);
        return registered == null ? registry.topicForType(type) : registered;
    }

    @Override
    public String topicFor(EventType<?> type) {
        var registered = bindings.get(type);
        return registered == null ? registry.topicForType(type) : registered;
    }

    @Override
    public void register(Class<?> type, String topic) {
        classes.put(type, topic);
    }

    @Override
    public void register(EventType<?> type, String topic) {
        bindings.put(type, topic);
    }
}
