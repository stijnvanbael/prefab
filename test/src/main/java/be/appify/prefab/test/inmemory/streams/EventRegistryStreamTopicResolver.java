package be.appify.prefab.test.inmemory.streams;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.engine.StreamTopicResolver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the topic of a stream value type from the {@link EventRegistry}, which is also where the in-memory bus
 * publishes the application's events. A topic that a stream wrote to by name takes precedence, so another stream can
 * read the type back from it.
 */
final class EventRegistryStreamTopicResolver implements StreamTopicResolver {
    private final EventRegistry eventRegistry;
    private final Map<Class<?>, String> classTopics = new ConcurrentHashMap<>();
    private final Map<String, String> eventTopics = new ConcurrentHashMap<>();

    EventRegistryStreamTopicResolver(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public String topicFor(Class<?> type) {
        var registered = classTopics.get(type);
        return registered != null ? registered : eventRegistry.topicForType(type);
    }

    @Override
    public String topicFor(EventType<?> type) {
        var registered = eventTopics.get(type.id());
        return registered != null ? registered : eventRegistry.topicForType(type);
    }

    @Override
    public void register(Class<?> type, String topic) {
        classTopics.put(type, topic);
    }

    @Override
    public void register(EventType<?> type, String topic) {
        eventTopics.put(type.id(), topic);
    }
}
