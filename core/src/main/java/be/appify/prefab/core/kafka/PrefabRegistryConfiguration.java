package be.appify.prefab.core.kafka;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring configuration that creates the single {@link EventRegistry} bean for the application.
 *
 * <p>All {@link EventRegistryCustomizer} beans discovered in the application context are collected
 * and applied atomically before the registry is exposed to any consumer. This guarantees that every
 * bean receiving an {@link EventRegistry} via constructor injection sees a fully-populated registry.
 *
 * <p>Annotating the bean with {@link ConditionalOnMissingBean} allows test configurations to
 * supply a pre-populated {@link EventRegistry} stub without conflicting with this factory.
 */
@AutoConfiguration
@ConditionalOnBean(EventRegistryCustomizer.class)
public class PrefabRegistryConfiguration {

    /** Constructs a new PrefabRegistryConfiguration. */
    public PrefabRegistryConfiguration() {
    }

    /**
     * Creates and populates the {@link EventRegistry} from all available {@link EventRegistryCustomizer} beans.
     *
     * @param customizers provider of all {@link EventRegistryCustomizer} beans in the context
     * @return a fully-populated {@link EventRegistry}
     */
    @Bean
    @ConditionalOnMissingBean(EventRegistry.class)
    public EventRegistry eventRegistry(ObjectProvider<EventRegistryCustomizer> customizers) {
        var registry = new EventRegistry();
        customizers.orderedStream().forEach(c -> c.customize(registry));
        return registry;
    }
}

