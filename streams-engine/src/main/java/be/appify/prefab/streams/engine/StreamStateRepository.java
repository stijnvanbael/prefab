package be.appify.prefab.streams.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Transactional storage SPI for engine state, deduplication and output delivery.
 *
 * <p>Implementations must use optimistic versions for state writes and make inbox, state and outbox changes atomic.
 * Logical state namespaces are used for stores, windows, joins and deduplication identities.
 */
public interface StreamStateRepository {
    /** Begins a transaction for one input message. */
    Transaction beginTransaction();

    /** Removes inbox entries past retention and releases expired relay leases using wall-clock time. */
    void cleanupExpired(Instant now, Duration inboxRetention);

    /** Claims pending outbox rows for a relay lease. */
    List<OutboxRecord> leaseOutbox(String topic, String owner, Instant now, Duration lease, int limit);

    /** Removes a published outbox row owned by the lease holder. */
    void completeOutbox(String owner, String recordId);

    /** Opens a transaction over repository state. */
    interface Transaction extends AutoCloseable {
        /**
         * Records an inbox identity. Returns false if the identity has already committed.
         */
        boolean recordInbox(String subscription, String messageId);

        /** Reads a value and captures its optimistic version. */
        Optional<Object> get(String namespace, Object key);

        /** Writes a value using the version captured by this transaction. */
        void put(String namespace, Object key, Object value);

        /** Writes a value with an event-time expiry and its operator key, using the captured version. */
        void putExpiring(String namespace, Object key, Object expiryKey, Object value, Instant expiresAt);

        /** Deletes values in an operator key whose event-time expiry is at or before its stream time. */
        void deleteExpired(String namespace, Object expiryKey, Instant streamTime);

        /** Deletes a value using the version captured by this transaction. */
        void delete(String namespace, Object key);

        /** Adds an output to the transactional outbox. */
        void enqueue(String topic, StreamMessage message);

        /** Commits all inbox, state and outbox changes atomically. */
        void commit();

        /** Rolls back any uncommitted changes. */
        void rollback();

        @Override
        default void close() {
            rollback();
        }
    }

    /** A committed message waiting for a sink publish. */
    record OutboxRecord(String id, String topic, StreamMessage message) {
    }

    /** Indicates a concurrent optimistic write conflict. */
    final class OptimisticConflictException extends RuntimeException {
        public OptimisticConflictException(String message) {
            super(message);
        }
    }
}
