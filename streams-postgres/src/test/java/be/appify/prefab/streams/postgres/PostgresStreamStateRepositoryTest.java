package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.engine.JsonStreamStateCodec;
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
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void aConflictOnOneStateRowCommitsNothingOfTheMessage() {
        var repository = PostgresTestDatabase.freshRepository();

        var outcome = commitWithConflictOnSecondRow(repository);

        assertThat(outcome).isInstanceOf(StreamStateRepository.OptimisticConflictException.class);
        assertNothingOfTheConflictingMessageCommitted(repository);
    }

    @Test
    void refusesATransactionManagerForAnotherDataSourceBeforeWritingAnything() {
        var repository = PostgresTestDatabase.freshRepository();
        // Two DataSource objects for one database: the JdbcTemplate would autocommit outside the transaction.
        var miswired = new PostgresStreamStateRepository(PostgresTestDatabase.jdbc(),
                new UnknownResourceTransactionManager(PostgresTestDatabase.newDataSource()),
                new JsonStreamStateCodec());

        var outcome = commitWithConflictOnSecondRow(miswired);

        assertThat(outcome).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("same DataSource");
        assertNothingOfTheConflictingMessageCommitted(repository);
    }

    @Test
    void rejectsADataSourceTransactionManagerForAnotherDataSource() {
        var jdbc = PostgresTestDatabase.jdbc();
        var transactionManager = new DataSourceTransactionManager(PostgresTestDatabase.newDataSource());
        var codec = new JsonStreamStateCodec();

        assertThatThrownBy(() -> new PostgresStreamStateRepository(jdbc, transactionManager, codec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same DataSource");
    }

    /** Commits a message that writes the inbox, two state rows and an outbox row; the second row is stale. */
    private static RuntimeException commitWithConflictOnSecondRow(StreamStateRepository repository) {
        var transaction = repository.beginTransaction();
        transaction.recordInbox("sales", "s-1");
        transaction.put("aggregate", "books", 5);
        transaction.get("join", "s-1");
        try (var concurrent = PostgresTestDatabase.newInstance().beginTransaction()) {
            concurrent.recordInbox("refunds", "r-1");
            concurrent.put("join", "s-1", "refund");
            concurrent.commit();
        }
        transaction.put("join", "s-1", "sale");
        transaction.enqueue("totals", new StreamMessage("s-1", "books", 5, Map.of(), Instant.EPOCH));
        try {
            transaction.commit();
            throw new AssertionError("The commit should have failed");
        } catch (RuntimeException failure) {
            transaction.rollback();
            return failure;
        }
    }

    private static void assertNothingOfTheConflictingMessageCommitted(StreamStateRepository repository) {
        try (var verification = repository.beginTransaction()) {
            assertThat(verification.recordInbox("sales", "s-1")).isTrue();
            assertThat(verification.get("aggregate", "books")).isEmpty();
            assertThat(verification.get("join", "s-1")).contains("refund");
            verification.rollback();
        }
        assertThat(repository.leaseOutbox("totals", "verifier", Instant.now(), Duration.ofSeconds(5), 10)).isEmpty();
    }

    /** A transaction manager whose resource the repository cannot inspect upfront, such as a JTA manager. */
    private static final class UnknownResourceTransactionManager implements PlatformTransactionManager {
        private final DataSourceTransactionManager delegate;

        private UnknownResourceTransactionManager(DataSource dataSource) {
            delegate = new DataSourceTransactionManager(dataSource);
        }

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return delegate.getTransaction(definition);
        }

        @Override
        public void commit(TransactionStatus status) {
            delegate.commit(status);
        }

        @Override
        public void rollback(TransactionStatus status) {
            delegate.rollback(status);
        }
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
