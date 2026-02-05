package be.appify.prefab.core.util;

import be.appify.prefab.core.annotations.Event;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * A registry that holds the serialization format for each topic. It allows registering and retrieving the serialization format for a given
 * topic. The serialization format is represented by the {@link Event.Serialization} enum.
 */
@Component
public class SerializationRegistry {
    private final Map<String, Event.Serialization> registry = new HashMap<>();

    /**
     * Registers the serialization format for a given topic.
     *
     * @param topic
     *         the topic for which to register the serialization format
     * @param serialization
     *         the serialization format to register for the topic
     */
    public void register(String topic, Event.Serialization serialization) {
        registry.put(topic, serialization);
    }

    /**
     * Retrieves the serialization format for a given topic.
     *
     * @param topic
     *         the topic for which to retrieve the serialization format
     * @return the serialization format registered for the topic
     * @throws IllegalStateException
     *         if no serialization format is registered for the topic
     */
    public Event.Serialization get(String topic) {
        if (!registry.containsKey(topic)) {
            throw new IllegalStateException("No serialization format registered for topic [%s]".formatted(topic));
        }
        return registry.get(topic);
    }
}
