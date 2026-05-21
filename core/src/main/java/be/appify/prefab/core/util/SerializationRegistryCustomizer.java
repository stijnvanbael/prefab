package be.appify.prefab.core.util;

import be.appify.prefab.core.kafka.EventRegistryCustomizer;

/**
 * @deprecated Use {@link EventRegistryCustomizer} instead.
 */
@Deprecated(since = "0.9", forRemoval = true)
@FunctionalInterface
public interface SerializationRegistryCustomizer {

    /**
     * @deprecated Implement {@link EventRegistryCustomizer} instead.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    void customize(SerializationRegistry registry);
}
