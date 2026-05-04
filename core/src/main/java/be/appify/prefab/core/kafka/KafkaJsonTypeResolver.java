package be.appify.prefab.core.kafka;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

/**
 * KafkaJsonTypeResolver resolves Java types for Kafka topics based on a registered mapping.
 */
@Component
public class KafkaJsonTypeResolver implements JacksonJsonTypeResolver {
    private final Map<String, Class<?>> types = new HashMap<>();
    private final Map<String, Set<Class<?>>> topicTypes = new ConcurrentHashMap<>();
    private final Set<String> allowedClassNames = ConcurrentHashMap.newKeySet();

    /** Constructs a new KafkaJsonTypeResolver. */
    public KafkaJsonTypeResolver() {
    }

    @Override
    public JavaType resolveType(String topic, byte[] data, Headers headers) {
        if (types.containsKey(topic)) {
            return TypeFactory.unsafeSimpleType(types.get(topic));
        } else {
            throw new IllegalArgumentException("No type registered for topic: " + topic);
        }
    }

    /**
     * Registers a Java type for a specific Kafka topic.
     * If the type is a sealed interface, all permitted subtypes are recursively added to the allowlist.
     *
     * @param topic the Kafka topic
     * @param type  the Java class type to register
     */
    public void registerType(String topic, Class<?> type) {
        types.put(topic, type);
        addToTopicTypes(topic, type);
    }

    private void addToTopicTypes(String topic, Class<?> type) {
        topicTypes.computeIfAbsent(topic, ignored -> new LinkedHashSet<>()).add(type);
        addToAllowedClassNames(type);
        if (type.isSealed()) {
            for (Class<?> subtype : type.getPermittedSubclasses()) {
                addToTopicTypes(topic, subtype);
            }
        }
    }

    /**
     * Returns registered event classes for a topic.
     *
     * @param topic
     *         the Kafka topic
     * @return registered classes for the topic, or an empty set if none were registered
     */
    public Set<Class<?>> registeredTypesForTopic(String topic) {
        return Set.copyOf(topicTypes.getOrDefault(topic, Set.of()));
    }

    private void addToAllowedClassNames(Class<?> type) {
        allowedClassNames.add(type.getName());
        if (type.isSealed()) {
            for (Class<?> subtype : type.getPermittedSubclasses()) {
                addToAllowedClassNames(subtype);
            }
        }
    }

    /**
     * Returns the set of all registered event class names for allowlist validation.
     *
     * @return set of fully-qualified class names of registered event types
     */
    public Set<String> allowedClassNames() {
        return allowedClassNames;
    }

    /**
     * Returns the set of all registered event classes.
     *
     * @return set of registered event classes
     */
    public Set<Class<?>> registeredTypes() {
        return Set.copyOf(types.values());
    }
}
