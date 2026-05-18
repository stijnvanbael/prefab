package be.appify.prefab.core.kafka;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

/**
 * Registry for event types, their associated topics/channels, and partitioning key extractors.
 *
 * <p>Acts as a central catalogue used at both publish time (topic and key resolution)
 * and consume time (JSON type resolution and class-name allowlisting).
 */
@Component
public class EventRegistry implements JacksonJsonTypeResolver {
    private final Map<String, Class<?>> types = new ConcurrentHashMap<>();
    private final Map<String, Set<Class<?>>> topicTypes = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<String>> typeTopics = new ConcurrentHashMap<>();
    private final Set<String> allowedClassNames = ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, Function<Object, String>> keyExtractors = new ConcurrentHashMap<>();

    /** Constructs a new EventRegistry. */
    public EventRegistry() {
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
     * Registers a Java type for a specific topic.
     * If the type is a sealed interface, all permitted subtypes are recursively added to the allowlist.
     *
     * @param topic the topic or channel name
     * @param type  the Java class type to register
     */
    public void registerType(String topic, Class<?> type) {
        types.put(topic, type);
        addToTopicTypes(topic, type);
    }

    /**
     * Registers a Java type for a specific topic together with a function that extracts the
     * partitioning key from an event of that type.
     *
     * @param <E>          the event type
     * @param topic        the topic or channel name
     * @param type         the Java class type to register
     * @param keyExtractor a function that returns the partitioning key for a given event instance
     */
    @SuppressWarnings("unchecked")
    public <E> void registerType(String topic, Class<E> type, Function<E, String> keyExtractor) {
        registerType(topic, type);
        keyExtractors.put(type, (Function<Object, String>) (Function<?, String>) keyExtractor);
    }

    /**
     * Returns the partitioning key for the given event, if a key extractor has been registered
     * for its type or any of its supertypes.
     *
     * @param event the event instance
     * @return an {@link Optional} containing the partitioning key, or empty if no extractor is registered
     */
    public Optional<String> keyFor(Object event) {
        return findExtractor(event.getClass()).map(extractor -> extractor.apply(event));
    }

    private Optional<Function<Object, String>> findExtractor(Class<?> type) {
        var extractor = keyExtractors.get(type);
        if (extractor != null) {
            return Optional.of(extractor);
        }
        for (var entry : keyExtractors.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private void addToTopicTypes(String topic, Class<?> type) {
        topicTypes.computeIfAbsent(topic, ignored -> new LinkedHashSet<>()).add(type);
        typeTopics.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(topic);
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
     * @param topic the topic or channel name
     * @return registered classes for the topic, or an empty set if none were registered
     */
    public Set<Class<?>> registeredTypesForTopic(String topic) {
        return Set.copyOf(topicTypes.getOrDefault(topic, Set.of()));
    }

    /**
     * Resolves the single registered topic for a Java type.
     *
     * @param type the event type
     * @return the single registered topic name
     * @throws IllegalArgumentException if no topic is registered for the type
     * @throws IllegalStateException    if multiple topics are registered for the type
     */
    public String topicForType(Class<?> type) {
        var topics = Set.copyOf(typeTopics.getOrDefault(type, Set.of()));
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("No topic registered for type: " + type.getName());
        }
        if (topics.size() > 1) {
            throw new IllegalStateException("Multiple topics registered for type " + type.getName() + ": " + topics);
        }
        return topics.iterator().next();
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
