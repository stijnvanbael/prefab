package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;

/** Resolves event type to topic for streams source/sink operations. */
public class KafkaTopicResolver {
    private final KafkaJsonTypeResolver typeResolver;

    /**
     * Constructs a new KafkaTopicResolver.
     *
     * @param typeResolver
     *         resolver that tracks Kafka type registrations
     */
    public KafkaTopicResolver(KafkaJsonTypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    /**
     * Resolves the topic for the given event class.
     *
     * @param type
     *         event class
     * @return topic name
     */
    public String topicForType(Class<?> type) {
        return typeResolver.topicForType(type);
    }
}

