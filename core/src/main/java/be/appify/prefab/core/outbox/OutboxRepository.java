package be.appify.prefab.core.outbox;

import java.util.List;

/**
 * Persistence contract for the transactional outbox.
 * Implementations are provided by the database-specific modules (JDBC, MongoDB, etc.).
 */
public interface OutboxRepository {

    /**
     * Persists a new outbox entry.
     *
     * @param entry the entry to save
     */
    void save(OutboxEntry entry);

    /**
     * Returns a batch of entries that have not yet been published (i.e. {@code publishedAt} is {@code null}).
     *
     * @param batchSize the maximum number of entries to return
     * @return pending outbox entries, oldest first
     */
    List<OutboxEntry> findPending(int batchSize);

    /**
     * Deletes the outbox entry with the given identifier.
     *
     * @param id the identifier of the entry to delete
     */
    void delete(String id);
}
