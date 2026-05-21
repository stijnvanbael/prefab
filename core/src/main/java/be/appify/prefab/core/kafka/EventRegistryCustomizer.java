package be.appify.prefab.core.kafka;

/**
 * Callback interface for customizing an {@link EventRegistry} at startup.
 *
 * <p>Implementations register event types and their serialization formats for one or more topics.
 * All beans of this type are automatically collected by {@code PrefabRegistryConfiguration} and
 * applied to the {@link EventRegistry} before it is exposed to any other bean, guaranteeing that
 * consumers always see a fully-populated registry.
 */
@FunctionalInterface
public interface EventRegistryCustomizer {

    /**
     * Customize the given {@link EventRegistry} by registering event types and serialization formats.
     *
     * @param registry the registry to customize
     */
    void customize(EventRegistry registry);
}

