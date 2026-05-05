package be.appify.prefab.core.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Scheduled relay that reads pending outbox entries and publishes them as Spring application events.
 * The relay runs at a fixed delay controlled by {@code prefab.outbox.poll-interval-ms}.
 * <p>
 * The {@link OutboxRepository} is resolved lazily via {@link ObjectProvider} so that this service can
 * be registered unconditionally — without relying on {@code @ConditionalOnBean} timing — and simply
 * skips each relay cycle when no repository is available.
 * </p>
 */
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final ReentrantLock lock = new ReentrantLock();
    private final Set<String> inFlightEntryIds = ConcurrentHashMap.newKeySet();
    private final ObjectProvider<OutboxRepository> outboxRepositoryProvider;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JsonMapper jsonMapper;
    private final OutboxProperties properties;

    /**
     * Constructs a new {@code OutboxRelayService}.
     *
     * @param outboxRepositoryProvider  provider for the outbox repository; may return {@code null} when
     *                                  no repository is configured
     * @param applicationEventPublisher Spring event publisher used to dispatch deserialised events
     * @param jsonMapper                JSON mapper used to deserialise event payloads
     * @param properties                outbox configuration properties
     */
    public OutboxRelayService(
            ObjectProvider<OutboxRepository> outboxRepositoryProvider,
            ApplicationEventPublisher applicationEventPublisher,
            JsonMapper jsonMapper,
            OutboxProperties properties
    ) {
        this.outboxRepositoryProvider = outboxRepositoryProvider;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    /**
     * Reads a batch of pending outbox entries, publishes each event, and removes the entry on success.
     * Failures for individual entries are logged as warnings so that other entries can still be processed.
     * Does nothing when no {@link OutboxRepository} bean is available.
     * <p>
     * The {@link ReentrantLock} is held <em>only</em> during the brief read-and-claim phase (a single
     * database query plus an in-memory set update, typically a few milliseconds).  Kafka I/O happens
     * entirely outside the lock, so the {@code @Scheduled} backup poller can always acquire the lock
     * and retry failed entries even while an {@code @Async} relay invocation is blocked on a slow broker.
     * </p>
     * <p>
     * The {@code inFlightEntryIds} set prevents two concurrent relay invocations from processing the
     * same entry: when a second invocation reads the outbox it finds the same pending entries, but the
     * {@link Set#add} call returns {@code false} for entries already in-flight, so they are filtered out.
     * Entries are removed from the set in a {@code finally} block whether the publish succeeds or fails,
     * which guarantees the {@code @Scheduled} poller can pick them up on the next cycle.
     * </p>
     */
    @Scheduled(fixedDelayString = "${prefab.outbox.poll-interval-ms:1000}")
    public void relayPendingEvents() {
        OutboxRepository outboxRepository = outboxRepositoryProvider.getIfAvailable();
        if (outboxRepository == null) {
            return;
        }

        List<OutboxEntry> entriesToProcess = claimPendingEntries(outboxRepository);
        if (entriesToProcess.isEmpty()) {
            return;
        }

        for (OutboxEntry entry : entriesToProcess) {
            processEntry(entry, outboxRepository);
        }
    }

    private List<OutboxEntry> claimPendingEntries(OutboxRepository outboxRepository) {
        if (!lock.tryLock()) {
            return List.of();
        }
        try {
            return outboxRepository.findPending(properties.getBatchSize()).stream()
                    .filter(entry -> inFlightEntryIds.add(entry.id()))
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    private void processEntry(OutboxEntry entry, OutboxRepository outboxRepository) {
        try {
            Class<?> eventType = Class.forName(entry.eventType());
            Object event = jsonMapper.readValue(entry.payload(), eventType);
            applicationEventPublisher.publishEvent(event);
            outboxRepository.delete(entry.id());
        } catch (Exception e) {
            log.warn("Failed to relay outbox entry {}: {}", entry.id(), e.getMessage(), e);
        } finally {
            inFlightEntryIds.remove(entry.id());
        }
    }

    /**
     * Triggered asynchronously after an outbox entry is persisted.
     * <p>
     * When a Spring transaction is active (e.g. a JDBC-backed aggregate save), this method fires
     * after the transaction commits, guaranteeing that the new entries are visible to the read
     * query in {@link #relayPendingEvents()}.  When there is no active transaction (e.g. a MongoDB
     * save without explicit transaction management), {@code fallbackExecution = true} causes this
     * listener to fire immediately.  The {@code @Async} annotation ensures the relay runs on a
     * separate thread so the calling request thread is never blocked by Kafka I/O.
     * </p>
     *
     * @param event the marker event signalling that at least one entry was added to the outbox
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOutboxEntryAdded(OutboxEntryAdded event) {
        relayPendingEvents();
    }
}
