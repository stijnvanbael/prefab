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
import java.util.concurrent.locks.ReentrantLock;

/**
 * Scheduled relay that reads pending outbox entries and publishes them as Spring application events.
 * The relay runs at a fixed delay controlled by {@code prefab.outbox.poll-interval-ms}.
 * <p>
 * The {@link OutboxRepository} is resolved lazily via {@link ObjectProvider} so that this service can
 * be registered unconditionally — without relying on {@code @ConditionalOnBean} timing — and simply
 * skips each relay cycle when no repository is available.
 * </p>
 * <p>
 * Entries are processed <em>strictly in insertion order</em> by a single relay thread at a time.
 * The {@link ReentrantLock} is held for the entire relay cycle, including Kafka I/O, so that
 * concurrent {@code @Async} invocations triggered by {@link OutboxEntryAdded} events immediately
 * yield when another relay is already running.  This preserves the causal ordering that consumers
 * depend on (e.g. an aggregate must be created before downstream events for it can be applied).
 * </p>
 * <p>
 * To avoid holding the lock indefinitely when the broker is unavailable, configure
 * {@code spring.kafka.producer.properties.max.block.ms} to a value appropriate for your
 * environment (e.g. 1000 ms in tests; a larger value in production that reflects acceptable
 * relay latency during a broker outage).  After a failed cycle the {@code @Scheduled} poller
 * retries at the next interval.
 * </p>
 */
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final ReentrantLock lock = new ReentrantLock();
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
     * The {@link ReentrantLock} is held for the entire duration of this method, including Kafka I/O,
     * ensuring that entries are delivered to the broker in insertion order.  If another invocation is
     * already running {@link java.util.concurrent.locks.ReentrantLock#tryLock()} returns {@code false}
     * and this invocation exits immediately; the running relay will process all currently visible
     * entries, and the {@code @Scheduled} backup poller will pick up any entries added afterwards.
     * </p>
     * <p>
     * If the broker is unavailable the loop stops at the first failed publish so that the lock is
     * released promptly and the {@code @Scheduled} poller can retry the whole batch at the next
     * interval without waiting for all remaining entries to time out.  Configure
     * {@code spring.kafka.producer.properties.max.block.ms} to a value appropriate for your
     * environment (e.g. 1000 ms in tests, a larger value in production) to bound the time the lock
     * is held during a broker outage.
     * </p>
     */
    @Scheduled(fixedDelayString = "${prefab.outbox.poll-interval-ms:1000}")
    public void relayPendingEvents() {
        OutboxRepository outboxRepository = outboxRepositoryProvider.getIfAvailable();
        if (outboxRepository == null) {
            return;
        }
        if (!lock.tryLock()) {
            return;
        }
        try {
            List<OutboxEntry> entries = outboxRepository.findPending(properties.getBatchSize());
            for (OutboxEntry entry : entries) {
                if (!processEntry(entry, outboxRepository)) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Processes a single outbox entry.
     *
     * @return {@code true} if processing should continue with subsequent entries;
     *         {@code false} if the broker appears to be unavailable and the loop should stop
     */
    private boolean processEntry(OutboxEntry entry, OutboxRepository outboxRepository) {
        Class<?> eventType;
        Object event;
        try {
            eventType = Class.forName(entry.eventType());
            event = jsonMapper.readValue(entry.payload(), eventType);
        } catch (Exception e) {
            log.warn("Failed to deserialise outbox entry {}: {}", entry.id(), e.getMessage(), e);
            return true;
        }
        try {
            applicationEventPublisher.publishEvent(event);
            outboxRepository.delete(entry.id());
            return true;
        } catch (Exception e) {
            log.warn("Failed to relay outbox entry {}: {}", entry.id(), e.getMessage(), e);
            return false;
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
