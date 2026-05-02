package be.appify.prefab.core.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Base Spring configuration for Prefab core components.
 * <p>
 * This configuration scans the core Spring package to register database-agnostic beans such as
 * {@link StorageService}, {@link SpringDomainEventPublisher}, and {@link SpringServiceLocator}.
 * It is imported by {@link EnablePrefab} and does not include any database-specific configuration.
 * </p>
 */
@Configuration
@ComponentScan("be.appify.prefab.core.spring")
public class PrefabCoreConfiguration {

    /**
     * Constructs a new PrefabCoreConfiguration.
     */
    public PrefabCoreConfiguration() {
    }
}
