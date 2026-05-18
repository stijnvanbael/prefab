package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.EventRegistry;

/** Resolves event type to topic for streams source/sink operations. */
public class KafkaTopicResolver {
    private final EventRegistry eventRegistry;

    /**
     * Constructs a new KafkaTopicResolver.
     *
     * @param eventRegistry
     *         registry that tracks event type registrations
     */
    public KafkaTopicResolver(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    /**
     * Resolves the topic for the given event class.
     *
     * @param type
     *         event class
     * @return topic name
     */
    public String topicForType(Class<?> type) {
        return eventRegistry.topicForType(type);
    }
}

