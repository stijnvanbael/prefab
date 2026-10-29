package be.appify.prefab.core.spring;

import be.appify.prefab.core.evolution.EventEvolutionModule;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.Migration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
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

    /**
     * Registers JSON support for event evolution annotations with the application's {@code JsonMapper}.
     *
     * @return the evolution module
     */
    @Bean
    public EventEvolutionModule eventEvolutionModule() {
        return new EventEvolutionModule();
    }

    /**
     * Collects the {@link Migration} beans into the migrator that reads messages written with older versions.
     *
     * @param migrations the migration beans
     * @return the event migrator
     */
    @Bean
    public EventMigrator eventMigrator(ObjectProvider<Migration<?, ?>> migrations) {
        return new EventMigrator(migrations.orderedStream().toList());
    }
}
