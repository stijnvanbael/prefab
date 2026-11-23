package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.engine.StreamStateRepositoryContractTest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresStreamStateRepositoryTest extends StreamStateRepositoryContractTest {
    @Override
    protected StreamStateRepository createRepository() {
        return PostgresTestDatabase.freshRepository();
    }

    @Test
    void concurrentIncrementsFromTwoInstancesLoseNoUpdate() throws Exception {
        PostgresTestDatabase.freshRepository();
        var instances = new StreamStateRepository[] {
                PostgresTestDatabase.newInstance(), PostgresTestDatabase.newInstance()};
        var perInstance = 50;
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<?>>();
            for (var instance : instances) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (var i = 0; i < perInstance; i++) {
                        incrementWithRetry(instance);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (var future : futures) {
                future.get();
            }
        }

        var verification = instances[0].beginTransaction();
        assertThat(verification.get("counts", "key")).contains(perInstance * instances.length);
        verification.rollback();
    }

    @Test
    void outputIsRelayedByAnotherInstanceAfterTheLeaseHolderCrashes() {
        var crashed = PostgresTestDatabase.freshRepository();
        var message = new StreamMessage("m1", "key", "value", Map.of("a", "b"), Instant.EPOCH);
        try (var transaction = crashed.beginTransaction()) {
            transaction.recordInbox("subscription", "m1");
            transaction.enqueue("output", message);
            transaction.commit();
        }
        var now = Instant.now();
        // The first instance leases the row and dies before publishing and completing it.
        assertThat(crashed.leaseOutbox("output", "crashed", now, Duration.ofSeconds(5), 10)).hasSize(1);
        var survivor = PostgresTestDatabase.newInstance();
        assertThat(survivor.leaseOutbox("output", "survivor", now, Duration.ofSeconds(5), 10)).isEmpty();

        var records = survivor.leaseOutbox("output", "survivor", now.plusSeconds(6), Duration.ofSeconds(5), 10);

        assertThat(records).singleElement().satisfies(record -> assertThat(record.message()).isEqualTo(message));
        survivor.completeOutbox("survivor", records.getFirst().id());
        assertThat(survivor.leaseOutbox("output", "survivor", now.plusSeconds(6), Duration.ofSeconds(5), 10))
                .isEmpty();
    }

    @Test
    void schemaInitializationIsIdempotent() {
        PostgresTestDatabase.freshRepository();

        PostgresStreamSchema.initialize(PostgresTestDatabase.jdbc());

        assertThat(PostgresTestDatabase.jdbc().queryForObject(
                "SELECT count(*) FROM prefab_stream_store", Integer.class)).isZero();
    }

    private static void incrementWithRetry(StreamStateRepository repository) {
        while (true) {
            try (var transaction = repository.beginTransaction()) {
                var current = transaction.get("counts", "key").map(Integer.class::cast).orElse(0);
                transaction.put("counts", "key", current + 1);
                transaction.commit();
                return;
            } catch (StreamStateRepository.OptimisticConflictException conflict) {
                // retry the whole message, as the engine does
            }
        }
    }
}
