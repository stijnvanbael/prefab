package be.appify.prefab.core.outbox;

/**
 * Application event published after one or more {@link OutboxEntry} records have been persisted.
 * <p>
 * {@link OutboxRelayService} listens to this event and immediately relays pending outbox entries
 * to Kafka, so that events are forwarded with minimal latency rather than waiting for the next
 * scheduled poll cycle.  The listener runs asynchronously (via {@code @Async}) on a separate
 * thread, and uses {@code @TransactionalEventListener} so that — when there is an active Spring
 * transaction — the relay only starts after the transaction has committed (ensuring the entries
 * are visible to the read query).  When no transaction is active (e.g. in the MongoDB
 * non-transactional path) {@code fallbackExecution = true} causes the listener to fire
 * immediately.
 * </p>
 */
public record OutboxEntryAdded() {
}
