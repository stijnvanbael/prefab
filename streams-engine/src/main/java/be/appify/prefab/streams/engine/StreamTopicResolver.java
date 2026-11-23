package be.appify.prefab.streams.engine;

import be.appify.prefab.core.domain.EventType;

/** Resolves the application topic associated with a stream value type. */
public interface StreamTopicResolver {
    /** Resolves a non-generic event class. */
    String topicFor(Class<?> type);

    /** Resolves a concrete event binding, including its generic arguments. */
    String topicFor(EventType<?> type);

    /** Registers an explicit output topic for a class binding when the resolver supports aliases. */
    default void register(Class<?> type, String topic) {
    }

    /** Registers an explicit output topic for a concrete binding when the resolver supports aliases. */
    default void register(EventType<?> type, String topic) {
    }
}
