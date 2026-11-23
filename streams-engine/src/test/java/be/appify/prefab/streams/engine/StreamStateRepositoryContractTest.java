package be.appify.prefab.streams.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reusable behavioral contract for stream state repository implementations.
 *
 * <p>Backend modules can extend this class and supply their repository implementation to run the same
 * transactional, inbox, outbox, lease, and expiry checks.
 */
public abstract class StreamStateRepositoryContractTest {
    protected abstract StreamStateRepository createRepository();

    @Test
    void optimisticWritesRejectAStaleVersion() {
        var repository = createRepository();
        var first = repository.beginTransaction();
        var second = repository.beginTransaction();
        first.get("counts", "key");
        second.get("counts", "key");
        first.put("counts", "key", 1);
        second.put("counts", "key", 2);

        first.commit();

        assertThatThrownBy(second::commit)
                .isInstanceOf(StreamStateRepository.OptimisticConflictException.class);
        var verification = repository.beginTransaction();
        assertThat(verification.get("counts", "key")).contains(1);
        verification.rollback();
    }

    @Test
    void inboxStateAndOutboxCommitAtomically() {
        var repository = createRepository();
        var message = new StreamMessage("message", "key", "value", Map.of(), Instant.EPOCH);
        var transaction = repository.beginTransaction();
        assertThat(transaction.recordInbox("topic", "message")).isTrue();
        transaction.put("counts", "key", 3);
        transaction.enqueue("output", message);
        transaction.commit();

        var duplicate = repository.beginTransaction();
        assertThat(duplicate.recordInbox("topic", "message")).isFalse();
        duplicate.rollback();
        var leased = repository.leaseOutbox("output", "test-owner", Instant.now(), Duration.ofSeconds(30), 10);
        var verification = repository.beginTransaction();

        assertThat(verification.get("counts", "key")).contains(3);
        assertThat(leased).extracting(StreamStateRepository.OutboxRecord::topic).containsExactly("output");
        verification.rollback();
    }

    @Test
    void outboxLeasePreventsConcurrentRelayAndRecoversAfterExpiry() {
        var repository = createRepository();
        var message = new StreamMessage("message", "key", "value", Map.of(), Instant.EPOCH);
        var transaction = repository.beginTransaction();
        transaction.enqueue("output", message);
        transaction.commit();
        var now = Instant.now();

        assertThat(repository.leaseOutbox("output", "first-owner", now, Duration.ofSeconds(1), 10)).hasSize(1);
        assertThat(repository.leaseOutbox("output", "second-owner", now, Duration.ofSeconds(1), 10)).isEmpty();
        var afterExpiry = now.plusSeconds(2);
        repository.cleanupExpired(afterExpiry, Duration.ofDays(7));

        assertThat(repository.leaseOutbox(
                "output", "second-owner", afterExpiry, Duration.ofSeconds(1), 10)).hasSize(1);
    }

    @Test
    void stateExpiryUsesOperatorStreamTimeWhileInboxUsesWallClockRetention() {
        var repository = createRepository();
        var transaction = repository.beginTransaction();
        assertThat(transaction.recordInbox("topic", "message")).isTrue();
        transaction.putExpiring("windows", "closed", "key", "state", Instant.EPOCH);
        transaction.commit();

        repository.cleanupExpired(Instant.now().plus(Duration.ofDays(8)), Duration.ofDays(7));

        var beforeWatermark = repository.beginTransaction();
        assertThat(beforeWatermark.get("windows", "closed")).contains("state");
        beforeWatermark.rollback();

        var cleanup = repository.beginTransaction();
        cleanup.deleteExpired("windows", "key", Instant.now().plus(Duration.ofDays(8)));
        cleanup.commit();

        var verification = repository.beginTransaction();
        assertThat(verification.get("windows", "closed")).isEmpty();
        assertThat(verification.recordInbox("topic", "message")).isTrue();
        verification.rollback();
    }
}
