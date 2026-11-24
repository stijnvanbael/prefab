package be.appify.prefab.streams.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    @Test
    void membershipExpiresAndGracefulRemovalUpdatesTheLiveRoster() {
        var repository = createRepository();
        var now = Instant.parse("2030-01-01T00:00:00Z");
        repository.heartbeatMember("group", "member-a", "topology-v1", now, Duration.ofSeconds(5));

        assertThat(repository.activeMembers("group", now.plusSeconds(4))).containsExactly("member-a");
        assertThat(repository.activeMembers("group", now.plusSeconds(7))).isEmpty();

        repository.heartbeatMember("group", "member-a", "topology-v1", now, Duration.ofSeconds(5));
        repository.removeMember("group", "member-a");

        assertThat(repository.activeMembers("group", now)).isEmpty();
    }

    @Test
    void concurrentMembersWithTheSameTopologyCanJoin() throws Exception {
        var repository = createRepository();
        var now = Instant.parse("2030-01-01T00:00:00Z");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<?>>();
            for (var index = 0; index < 8; index++) {
                var memberId = "member-" + index;
                futures.add(executor.submit(() -> {
                    start.await();
                    repository.heartbeatMember("group", memberId, "topology-v1", now, Duration.ofSeconds(5));
                    return null;
                }));
            }
            start.countDown();
            for (var future : futures) {
                future.get();
            }
        }

        assertThat(repository.activeMembers("group", now)).hasSize(8);
    }

    @Test
    void topologyDriftIsRejectedUntilTheOldMembershipLeaves() {
        var repository = createRepository();
        var now = Instant.parse("2030-01-01T00:00:00Z");
        repository.heartbeatMember("group", "member-a", "topology-v1", now, Duration.ofSeconds(5));
        repository.registerScheduledTask("group", "schedule", Duration.ofSeconds(1), now);
        repository.claimScheduledTask(
                "group", "schedule", "member-a", now.plusSeconds(1), Duration.ofSeconds(5)).orElseThrow();

        assertThatThrownBy(() -> repository.heartbeatMember(
                "group", "member-b", "topology-v2", now.plusSeconds(1), Duration.ofSeconds(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Topology declaration changed");

        repository.removeMember("group", "member-a");
        assertThatThrownBy(() -> repository.heartbeatMember(
                "group", "member-b", "topology-v2", now.plusSeconds(1), Duration.ofSeconds(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Topology declaration changed");
        repository.heartbeatMember("group", "member-b", "topology-v2", now.plusSeconds(10), Duration.ofSeconds(5));

        assertThat(repository.activeMembers("group", now.plusSeconds(10))).containsExactly("member-b");
    }

    @Test
    void scheduleLeaseFencesStaleOwnersFromCommittingStateAndOutbox() {
        var repository = createRepository();
        var registeredAt = Instant.parse("2030-01-01T00:00:00Z");
        var dueAt = registeredAt.plusSeconds(1);
        repository.registerScheduledTask("group", "schedule", Duration.ofSeconds(1), registeredAt);
        var firstOwner = repository.claimScheduledTask(
                "group", "schedule", "member-a", dueAt, Duration.ofSeconds(2)).orElseThrow();
        assertThat(repository.claimScheduledTask(
                "group", "schedule", "member-b", dueAt.plusSeconds(1), Duration.ofSeconds(2))).isEmpty();

        var staleTransaction = repository.beginTransaction();
        staleTransaction.put("punctuation", "count", 1);
        staleTransaction.enqueue("output", new StreamMessage("stale", "key", "stale", Map.of(), dueAt));
        staleTransaction.completeScheduledTask(firstOwner, dueAt.plusSeconds(1), dueAt.plusSeconds(4));

        var secondOwner = repository.claimScheduledTask(
                "group", "schedule", "member-b", dueAt.plusSeconds(4), Duration.ofSeconds(5)).orElseThrow();
        assertThat(secondOwner.fencingToken()).isGreaterThan(firstOwner.fencingToken());

        assertThatThrownBy(staleTransaction::commit)
                .isInstanceOf(StreamStateRepository.ScheduleLeaseLostException.class);
        staleTransaction.rollback();
        var verification = repository.beginTransaction();
        assertThat(verification.get("punctuation", "count")).isEmpty();
        verification.rollback();

        var currentTransaction = repository.beginTransaction();
        currentTransaction.put("punctuation", "count", 1);
        currentTransaction.enqueue("output", new StreamMessage("current", "key", "current", Map.of(), dueAt));
        currentTransaction.completeScheduledTask(
                secondOwner, dueAt.plusSeconds(5), dueAt.plusSeconds(4));
        currentTransaction.commit();

        var committed = repository.beginTransaction();
        assertThat(committed.get("punctuation", "count")).contains(1);
        committed.rollback();
        assertThat(repository.leaseOutbox(
                "output", "relay", dueAt.plusSeconds(4), Duration.ofSeconds(10), 10))
                .extracting(record -> record.message().value()).containsExactly("current");
    }
}
