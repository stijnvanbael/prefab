package be.appify.prefab.core.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Scheduled relay that reads pending outbox entries and publishes them as Spring application events.
 * The relay runs at a fixed delay controlled by {@code prefab.outbox.poll-interval-ms}.
 */
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JsonMapper jsonMapper;
    private final OutboxProperties properties;

    /**
     * Constructs a new {@code OutboxRelayService}.
     *
     * @param outboxRepository         repository used to read and delete outbox entries
     * @param applicationEventPublisher Spring event publisher used to dispatch deserialised events
     * @param jsonMapper               JSON mapper used to deserialise event payloads
     * @param properties               outbox configuration properties
     */
    public OutboxRelayService(
            OutboxRepository outboxRepository,
            ApplicationEventPublisher applicationEventPublisher,
            JsonMapper jsonMapper,
            OutboxProperties properties
    ) {
        this.outboxRepository = outboxRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    /**
     * Reads a batch of pending outbox entries, publishes each event, and removes the entry on success.
     * Failures for individual entries are logged as warnings so that other entries can still be processed.
     */
    @Scheduled(fixedDelayString = "${prefab.outbox.poll-interval-ms:1000}")
    public void relayPendingEvents() {
        List<OutboxEntry> entries = outboxRepository.findPending(properties.getBatchSize());
        for (OutboxEntry entry : entries) {
            try {
                Class<?> eventType = Class.forName(entry.eventType());
                Object event = jsonMapper.readValue(entry.payload(), eventType);
                applicationEventPublisher.publishEvent(event);
                outboxRepository.delete(entry.id());
            } catch (Exception e) {
                log.warn("Failed to relay outbox entry {}: {}", entry.id(), e.getMessage(), e);
            }
        }
    }
}
