package be.appify.prefab.core.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;


/**
 * Spring auto-configuration that activates the transactional outbox relay.
 * The relay is only started when an {@link OutboxRepository} bean is present in the application context,
 * allowing modules without an outbox repository to skip this configuration entirely.
 * <p>
 * Registered as an auto-configuration and ordered after the database-specific configurations so that
 * the {@link OutboxRepository} implementation is already registered when the condition is evaluated.
 * </p>
 */
@AutoConfiguration(afterName = {
        // String-based references are intentional: core does not depend on mongodb or postgres modules.
        // These names must be updated if the respective configuration classes are renamed or moved.
        "be.appify.prefab.mongodb.spring.PrefabMongoConfiguration",
        "be.appify.prefab.postgres.spring.PrefabJdbcConfiguration"
})
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
public class OutboxConfiguration {

    /**
     * Creates the {@link OutboxRelayService} bean.
     * <p>
     * The {@link ConditionalOnBean} annotation is placed on this method (not the class) so that the
     * condition is evaluated after <em>all</em> configuration classes have been processed and their bean
     * definitions registered — including the database-specific configurations that register the
     * {@link OutboxRepository} implementation via {@code @ComponentScan}. A class-level condition would
     * be evaluated too early (when the configuration class is first registered), before the
     * {@code @ComponentScan} in {@code PrefabMongoConfiguration} / {@code PrefabJdbcConfiguration} has
     * had a chance to register the repository bean definition.
     * </p>
     *
     * @param outboxRepository         repository for reading and deleting outbox entries
     * @param applicationEventPublisher Spring event publisher for dispatching relayed events
     * @param jsonMapper               JSON mapper for deserialising event payloads
     * @param properties               outbox configuration properties
     * @return the configured relay service
     */
    @Bean
    @ConditionalOnBean(OutboxRepository.class)
    public OutboxRelayService outboxRelayService(
            OutboxRepository outboxRepository,
            ApplicationEventPublisher applicationEventPublisher,
            JsonMapper jsonMapper,
            OutboxProperties properties
    ) {
        return new OutboxRelayService(outboxRepository, applicationEventPublisher, jsonMapper, properties);
    }
}
