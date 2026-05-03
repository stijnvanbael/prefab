package be.appify.prefab.core.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Base Spring configuration for Prefab core components.
 * <p>
 * This configuration scans the core Spring package to register database-agnostic beans such as
 * {@link StorageService}, {@link SpringDomainEventPublisher}, and {@link SpringServiceLocator}.
 * It is imported by {@link EnablePrefab} and does not include any database-specific configuration.
 * </p>
 * <p>
 * {@link EnableScheduling} and {@link EnableAsync} are declared here (rather than only in
 * {@link be.appify.prefab.core.outbox.OutboxConfiguration}) so that the
 * {@code ScheduledAnnotationBeanPostProcessor} and {@code AsyncAnnotationBeanPostProcessor}
 * are registered early in the context lifecycle — before auto-configuration beans such as
 * {@link be.appify.prefab.core.outbox.OutboxRelayService} are created — guaranteeing that
 * the relay's {@code @Scheduled} and {@code @Async} methods are always picked up.
 * </p>
 */
@Configuration
@ComponentScan("be.appify.prefab.core.spring")
@EnableScheduling
@EnableAsync
public class PrefabCoreConfiguration {

    /**
     * Constructs a new PrefabCoreConfiguration.
     */
    public PrefabCoreConfiguration() {
    }
}
