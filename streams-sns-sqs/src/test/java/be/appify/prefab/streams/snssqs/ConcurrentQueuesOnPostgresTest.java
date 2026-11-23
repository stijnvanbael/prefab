package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import be.appify.prefab.streams.postgres.PostgresStreamSchema;
import be.appify.prefab.streams.postgres.PostgresStreamStateRepository;
import be.appify.prefab.streams.snssqs.StreamFixtures.CategoryTotal;
import be.appify.prefab.streams.snssqs.StreamFixtures.Refund;
import be.appify.prefab.streams.snssqs.StreamFixtures.RefundedSale;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import be.appify.prefab.streams.tracing.TracingConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs the queues of a topology at the same time on Postgres, as the pollers of one instance do, and checks that no
 * join match is lost and that every output reaches SNS without waiting for the next message.
 *
 * <p>Uses Testcontainers, or the database named by {@code PREFAB_TEST_POSTGRES_URL} (with
 * {@code PREFAB_TEST_POSTGRES_USER} and {@code PREFAB_TEST_POSTGRES_PASSWORD}) where Docker is unavailable.
 */
@EnabledIf("postgresAvailable")
class ConcurrentQueuesOnPostgresTest {
    private static PostgreSQLContainer container;
    private static DataSource dataSource;

    private final FakeSnsSqs aws = new FakeSnsSqs();
    private final EventRegistry registry = new EventRegistry();
    private InProcessPrefabStreams streams;

    static boolean postgresAvailable() {
        return System.getenv("PREFAB_TEST_POSTGRES_URL") != null
                || DockerClientFactory.instance().isDockerAvailable();
    }

    @BeforeAll
    static void startPostgres() {
        var source = new PGSimpleDataSource();
        var externalUrl = System.getenv("PREFAB_TEST_POSTGRES_URL");
        if (externalUrl != null) {
            source.setUrl(externalUrl);
            source.setUser(System.getenv("PREFAB_TEST_POSTGRES_USER"));
            source.setPassword(System.getenv("PREFAB_TEST_POSTGRES_PASSWORD"));
        } else {
            container = new PostgreSQLContainer("postgres:16-alpine");
            container.start();
            source.setUrl(container.getJdbcUrl());
            source.setUser(container.getUsername());
            source.setPassword(container.getPassword());
        }
        dataSource = source;
    }

    @AfterAll
    static void stopPostgres() {
        if (container != null) {
            container.stop();
        }
    }

    @BeforeEach
    void freshTopology() {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS prefab_stream_store, prefab_stream_inbox, "
                + "prefab_stream_outbox, prefab_stream_lease");
        PostgresStreamSchema.initialize(jdbc);
        var repository = new PostgresStreamStateRepository(
                jdbc, new DataSourceTransactionManager(dataSource), new JsonStreamStateCodec());
        registry.register("it-sales", Sale.class, Event.Serialization.JSON, Sale::saleId);
        registry.register("it-refunds", Refund.class, Event.Serialization.JSON, Refund::saleId);
        registry.register("it-refunded-sales", RefundedSale.class, Event.Serialization.JSON, RefundedSale::saleId);
        registry.register("it-category-totals", CategoryTotal.class, Event.Serialization.JSON,
                CategoryTotal::category);
        streams = new InProcessPrefabStreams(
                new SnsSqsStreamSource(aws.sqsUtil(), registry, "it", Duration.ofSeconds(60)),
                new SnsSqsStreamSink(aws.snsTemplate(), aws.sqsUtil(), aws.serializer(), registry),
                repository, new SnsSqsTopicResolver(registry), TracingConfig.withTracing(), Duration.ofDays(1));
    }

    @Test
    void aJoinMatchesWhicheverSideIsProcessedFirst() {
        joinSalesWithRefunds();
        try (var ignored = streams.start()) {
            var eventTime = Map.of("prefab-event-time", Instant.now().toString());
            var rounds = 150;
            for (var round = 0; round < rounds; round++) {
                aws.publish("it-sales", new Sale("s-" + round, "books", 1), eventTime, "s-" + round);
                aws.publish("it-refunds", new Refund("s-" + round, "damaged"), eventTime, "s-" + round);
            }

            aws.drainConcurrently();

            assertThat(aws.publications("it-refunded-sales")).hasSize(rounds);
        }
    }

    @Test
    void everyOutputIsPublishedWithoutWaitingForTheNextMessage() {
        joinSalesWithRefunds();
        streams.from(Sale.class)
                .groupBy(String.class, Sale::category)
                .aggregate(CategoryTotal.class, () -> new CategoryTotal(null, 0),
                        (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                .to(CategoryTotal.class);
        try (var ignored = streams.start()) {
            var eventTime = Map.of("prefab-event-time", Instant.now().toString());
            for (var round = 0; round < 60; round++) {
                aws.publish("it-sales", new Sale("a-" + round, "c" + round, 5), eventTime, "a-" + round);
                aws.publish("it-sales", new Sale("b-" + round, "c" + round, 10), eventTime, "b-" + round);
                aws.publish("it-refunds", new Refund("a-" + round, "damaged"), eventTime, "a-" + round);

                aws.drainConcurrently();

                assertThat(aws.publications("it-refunded-sales")).as("refunds after round %d", round)
                        .hasSize(round + 1);
                assertThat(aws.publications("it-category-totals")).as("totals after round %d", round)
                        .hasSize((round + 1) * 2);
            }
        }
    }

    private void joinSalesWithRefunds() {
        streams.from(Sale.class)
                .join(streams.from(Refund.class), JoinWindow.of(Duration.ofMinutes(5), Duration.ZERO),
                        (sale, refund) -> new RefundedSale(sale.saleId(), sale.category(), refund.reason()))
                .to(RefundedSale.class);
    }
}
