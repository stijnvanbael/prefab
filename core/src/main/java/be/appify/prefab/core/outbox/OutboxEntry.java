package be.appify.prefab.core.outbox;

import jakarta.annotation.Nullable;

import java.time.Instant;

/**
 * Represents a single domain event stored in the transactional outbox.
 * An entry is created in the same transaction as the aggregate state change and
 * deleted once the relay has successfully published the event to the broker.
 *
 * @param id            unique identifier of the outbox entry
 * @param aggregateType fully-qualified class name of the aggregate that produced the event
 * @param aggregateId   string representation of the aggregate's identifier
 * @param eventType     fully-qualified class name of the domain event
 * @param payload       JSON-serialised event payload
 * @param createdAt     timestamp at which the entry was created
 * @param publishedAt   timestamp at which the event was published; {@code null} if not yet published
 */
public record OutboxEntry(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        Instant createdAt,
        @Nullable Instant publishedAt
) {
}
