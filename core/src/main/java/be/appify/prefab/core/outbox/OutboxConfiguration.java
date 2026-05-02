package be.appify.prefab.core.outbox;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;


/**
 * Spring auto-configuration that activates the transactional outbox relay.
 * <p>
 * The {@link OutboxRelayService} is registered unconditionally: it resolves the {@link OutboxRepository}
 * lazily at each relay cycle via {@link ObjectProvider}, and simply skips the cycle when no repository
 * is available. This avoids relying on {@code @ConditionalOnBean} timing, which is unreliable when the
 * repository bean is registered by a concurrent or later-ordered auto-configuration class.
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
public class OutboxConfiguration {

    /**
     * Creates the {@link OutboxRelayService} bean.
     * <p>
     * The repository is injected as an {@link ObjectProvider} so that the relay bean can be registered
     * unconditionally: when no {@link OutboxRepository} is available in the context the relay simply
     * no-ops on every scheduled cycle.
     * </p>
     *
     * @param outboxRepositoryProvider  provider for the optional outbox repository
     * @param applicationEventPublisher Spring event publisher for dispatching relayed events
     * @param jsonMapper               JSON mapper for deserialising event payloads
     * @param properties               outbox configuration properties
     * @return the configured relay service
     */
    @Bean
    public OutboxRelayService outboxRelayService(
            ObjectProvider<OutboxRepository> outboxRepositoryProvider,
            ApplicationEventPublisher applicationEventPublisher,
            JsonMapper jsonMapper,
            OutboxProperties properties
    ) {
        return new OutboxRelayService(outboxRepositoryProvider, applicationEventPublisher, jsonMapper, properties);
    }
}
