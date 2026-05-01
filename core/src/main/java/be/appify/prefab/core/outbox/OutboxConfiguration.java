package be.appify.prefab.core.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;


/**
 * Spring configuration that activates the transactional outbox relay.
 * The relay is only started when an {@link OutboxRepository} bean is present in the application context,
 * allowing modules without an outbox repository to skip this configuration entirely.
 */
@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
@ConditionalOnBean(OutboxRepository.class)
public class OutboxConfiguration {

    /**
     * Creates the {@link OutboxRelayService} bean.
     *
     * @param outboxRepository         repository for reading and deleting outbox entries
     * @param applicationEventPublisher Spring event publisher for dispatching relayed events
     * @param jsonMapper               JSON mapper for deserialising event payloads
     * @param properties               outbox configuration properties
     * @return the configured relay service
     */
    @Bean
    public OutboxRelayService outboxRelayService(
            OutboxRepository outboxRepository,
            ApplicationEventPublisher applicationEventPublisher,
            JsonMapper jsonMapper,
            OutboxProperties properties
    ) {
        return new OutboxRelayService(outboxRepository, applicationEventPublisher, jsonMapper, properties);
    }
}
