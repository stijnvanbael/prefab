package be.appify.prefab.core.util;

/**
 * A customizer for {@link SerializationRegistry}. Implementations register serialization formats for one or more
 * topics. All beans of this type are automatically applied to the {@link SerializationRegistry} during startup.
 */
@FunctionalInterface
public interface SerializationRegistryCustomizer {

    /**
     * Customize the given {@link SerializationRegistry} by registering serialization formats for topics.
     *
     * @param registry
     *         the registry to customize
     */
    void customize(SerializationRegistry registry);
}

