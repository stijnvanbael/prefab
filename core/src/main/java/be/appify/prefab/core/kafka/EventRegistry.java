package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PublishTo;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Central registry for event types, their associated topics/channels, partitioning key extractors,
 * and serialization formats.
 *
 * <p>Acts as the single source of truth used at both publish time (topic, key and serialization
 * format resolution) and consume time (JSON type resolution and class-name allowlisting).
 *
 * <p>Instances are populated during application startup via {@link EventRegistryCustomizer} beans
 * collected by {@code PrefabRegistryConfiguration}. Once the bean is returned by that
 * configuration class all customizers have been applied atomically, so any bean that receives an
 * {@code EventRegistry} via constructor injection is guaranteed to see a fully-populated registry.
 */
public class EventRegistry {
    private final Map<String, Class<?>> types = new ConcurrentHashMap<>();
    private final Map<String, Set<Class<?>>> topicTypes = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<String>> typeTopics = new ConcurrentHashMap<>();
    private final Set<String> allowedClassNames = ConcurrentHashMap.newKeySet();
    private final Map<String, Class<?>> classNameToType = new ConcurrentHashMap<>();
    private final Map<Class<?>, Function<Object, String>> keyExtractors = new ConcurrentHashMap<>();
    private final Map<String, Event.Serialization> serializations = new ConcurrentHashMap<>();
    private final Map<Class<?>, PublishTo> publishToStrategies = new ConcurrentHashMap<>();

    /** Constructs a new EventRegistry. */
    public EventRegistry() {
    }

    /**
     * Returns the Java class registered for the given topic.
     *
     * @param topic the topic or channel name
     * @return the registered Java class
     * @throws IllegalArgumentException if no type is registered for the topic
     */
    public Class<?> typeFor(String topic) {
        var type = types.get(topic);
        if (type == null) {
            throw new IllegalArgumentException("No type registered for topic: " + topic);
        }
        return type;
    }

    /**
     * Returns whether a Java type is registered for the given topic.
     *
     * @param topic the topic or channel name
     * @return {@code true} if a type has been registered for the topic
     */
    public boolean hasTypeForTopic(String topic) {
        return types.containsKey(topic);
    }

    /**
     * Registers a Java type and its serialization format for a specific topic.
     *
     * @param <E>           the event type
     * @param topic         the topic or channel name
     * @param type          the Java class type to register
     * @param serialization the serialization format used for this topic
     */
    public <E> void register(String topic, Class<E> type, Event.Serialization serialization) {
        registerType(topic, type);
        serializations.put(topic, serialization);
    }

    /**
     * Registers a Java type, serialization format, and key extractor for a specific topic.
     *
     * @param <E>           the event type
     * @param topic         the topic or channel name
     * @param type          the Java class type to register
     * @param serialization the serialization format used for this topic
     * @param keyExtractor  a function that returns the partitioning key for a given event instance
     */
    public <E> void register(String topic, Class<E> type, Event.Serialization serialization,
                             Function<E, String> keyExtractor) {
        registerType(topic, type, keyExtractor);
        serializations.put(topic, serialization);
    }

    /**
     * Registers the serialization format for a topic without an associated Java type.
     * Useful for transports where type resolution is handled externally (e.g. PubSub, SNS/SQS).
     *
     * @param topic         the topic or channel name
     * @param serialization the serialization format used for this topic
     */
    public void register(String topic, Event.Serialization serialization) {
        serializations.put(topic, serialization);
    }

    /**
     * Returns whether a serialization format is registered for the given topic.
     *
     * @param topic the topic or channel name
     * @return {@code true} if a serialization format has been registered for the topic
     */
    public boolean contains(String topic) {
        return serializations.containsKey(topic);
    }

    /**
     * Returns the serialization format registered for the given topic.
     *
     * @param topic the topic or channel name
     * @return the registered serialization format
     * @throws IllegalStateException if no serialization format is registered for the topic
     */
    public Event.Serialization serialization(String topic) {
        var result = serializations.get(topic);
        if (result == null) {
            throw new IllegalStateException("No serialization format registered for topic [%s]".formatted(topic));
        }
        return result;
    }

    /**
     * Returns whether any topic is registered with the given serialization format.
     *
     * @param serialization the serialization format to check
     * @return {@code true} if at least one topic uses this serialization format
     */
    public boolean hasSerialization(Event.Serialization serialization) {
        return serializations.values().stream().anyMatch(s -> s == serialization);
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
        Function<Object, String> selected = null;
        Integer selectedDistance = null;
        for (var entry : keyExtractors.entrySet()) {
            if (!entry.getKey().isAssignableFrom(type)) {
                continue;
            }
            var distance = hierarchyDistance(type, entry.getKey());
            if (distance.isEmpty()) {
                continue;
            }
            if (selectedDistance == null || distance.get() < selectedDistance) {
                selectedDistance = distance.get();
                selected = entry.getValue();
            } else if (distance.get().equals(selectedDistance) && !entry.getValue().equals(selected)) {
                throw new IllegalStateException("Ambiguous key extractors registered for type: " + type.getName());
            }
        }
        return Optional.ofNullable(selected);
    }

    private static Optional<Integer> hierarchyDistance(Class<?> source, Class<?> target) {
        if (source.equals(target)) {
            return Optional.of(0);
        }
        if (!target.isAssignableFrom(source)) {
            return Optional.empty();
        }
        var queue = new ArrayDeque<Class<?>>();
        var distances = new HashMap<Class<?>, Integer>();
        queue.add(source);
        distances.put(source, 0);
        while (!queue.isEmpty()) {
            var current = queue.remove();
            var distance = distances.get(current);
            if (current.equals(target)) {
                return Optional.of(distance);
            }
            var superclass = current.getSuperclass();
            if (superclass != null && distances.putIfAbsent(superclass, distance + 1) == null) {
                queue.add(superclass);
            }
            for (var iface : current.getInterfaces()) {
                if (distances.putIfAbsent(iface, distance + 1) == null) {
                    queue.add(iface);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the single registered topic for a Java type, if one is registered.
     *
     * @param type the event type
     * @return an {@link Optional} containing the topic, or empty if none are registered
     * @throws IllegalStateException if multiple topics are registered for the type
     */
    public Optional<String> tryTopicForType(Class<?> type) {
        var topics = Set.copyOf(typeTopics.getOrDefault(type, Set.of()));
        if (topics.isEmpty()) {
            return Optional.empty();
        }
        if (topics.size() > 1) {
            throw new IllegalStateException("Multiple topics registered for type " + type.getName() + ": " + topics);
        }
        return Optional.of(topics.iterator().next());
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
     * Returns whether at least one topic is registered for the given Java type.
     *
     * @param type the event type
     * @return {@code true} if a topic has been registered for the type
     */
    public boolean hasTopicForType(Class<?> type) {
        return !typeTopics.getOrDefault(type, Set.of()).isEmpty();
    }

    /**
     * Returns all topics registered for the given Java type.
     *
     * @param type the event type
     * @return an immutable set of registered topic names; empty if none are registered
     */
    public Set<String> topicsForType(Class<?> type) {
        return Set.copyOf(typeTopics.getOrDefault(type, Set.of()));
    }

    /**
     * Stores the publish-to strategy for an event type.
     * Called by generated registrar beans during application startup.
     *
     * @param type      the event type
     * @param publishTo the strategy that governs which topics are targeted at dispatch time
     */
    public void registerPublishTo(Class<?> type, PublishTo publishTo) {
        publishToStrategies.put(type, publishTo);
    }

    /**
     * Resolves the topics to which the given event should be dispatched, applying the registered
     * {@link PublishTo} strategy. Defaults to {@link PublishTo#FIRST} when no strategy is registered.
     *
     * @param event the event instance
     * @return the ordered list of target topic names
     * @throws IllegalArgumentException if no topics are registered for the event type
     */
    public List<String> topicsForDispatch(Object event) {
        var type = event.getClass();
        var topics = typeTopics.getOrDefault(type, Set.of());
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("No topics registered for type: " + type.getName());
        }

        var strategy = publishToStrategies.get(type);
        if (strategy == null) {
            PublishTo selected = null;
            Integer selectedDistance = null;
            for (var entry : publishToStrategies.entrySet()) {
                if (!entry.getKey().isAssignableFrom(type)) {
                    continue;
                }
                var distance = hierarchyDistance(type, entry.getKey());
                if (distance.isEmpty()) {
                    continue;
                }
                if (selectedDistance == null || distance.get() < selectedDistance) {
                    selectedDistance = distance.get();
                    selected = entry.getValue();
                } else if (distance.get().equals(selectedDistance) && entry.getValue() != selected) {
                    throw new IllegalStateException("Ambiguous publishTo strategies registered for type: " + type.getName());
                }
            }
            strategy = selected != null ? selected : PublishTo.FIRST;
        }

        return switch (strategy) {
            case ALL -> List.copyOf(topics);
            case FIRST -> List.of(topics.iterator().next());
        };
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
        classNameToType.put(type.getName(), type);
        if(type.isInterface()) {
            if (type.isSealed()) {
                for (Class<?> subtype : type.getPermittedSubclasses()) {
                    addToAllowedClassNames(subtype);
                }
            } else {
                throw new IllegalArgumentException("Only sealed interfaces are supported for event types: " + type.getName());
            }
        }
    }

    /**
     * Looks up the registered Java class for the given fully-qualified class name.
     *
     * <p>Only classes that have been registered via {@link #registerType} are resolvable.
     * This method is the safe alternative to reflection-based class loading for deserialization allowlist checks.
     *
     * @param className the fully-qualified class name
     * @return an {@link Optional} containing the class, or empty if not registered
     */
    public Optional<Class<?>> typeByClassName(String className) {
        return Optional.ofNullable(classNameToType.get(className));
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
