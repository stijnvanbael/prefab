package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsDeserializer;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import be.appify.prefab.streams.postgres.PostgresStreamSchema;
import be.appify.prefab.streams.postgres.PostgresStreamStateRepository;
import be.appify.prefab.streams.snssqs.StreamFixtures.CategoryTotal;
import be.appify.prefab.streams.snssqs.StreamFixtures.Product;
import be.appify.prefab.streams.snssqs.StreamFixtures.Refund;
import be.appify.prefab.streams.snssqs.StreamFixtures.RefundedSale;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import be.appify.prefab.streams.tracing.TracingConfig;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the streams backend end to end on LocalStack (SNS/SQS, FIFO topics) and Postgres: {@code groupBy} with
 * {@code aggregate}, {@code windowedAggregate}, {@code join} and a global store, plus a redelivered message.
 *
 * <p>Needs Docker for LocalStack. Postgres comes from Testcontainers, or from {@code PREFAB_TEST_POSTGRES_URL}
 * (with {@code PREFAB_TEST_POSTGRES_USER} and {@code PREFAB_TEST_POSTGRES_PASSWORD}).
 */
@EnabledIf("dockerAvailable")
class SnsSqsStreamsIntegrationTest {
    private static LocalStackContainer localstack;
    private static PostgreSQLContainer postgres;

    private static JdbcTemplate stateJdbc;
    private static SqsAsyncClient queueClient;
    private static SqsUtil sqsUtil;
    private static SnsTemplate snsTemplate;
    private static SnsSqsStreamSink sink;
    private static EventRegistry registry;
    private static InProcessPrefabStreams.RunningTopology topology;
    private static GlobalStore<String, Product> products;
    private static final List<CategoryTotal> totals = new CopyOnWriteArrayList<>();
    private static final List<CategoryTotal> windowedTotals = new CopyOnWriteArrayList<>();
    private static final List<RefundedSale> refunded = new CopyOnWriteArrayList<>();

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @BeforeAll
    static void startInfrastructure() {
        localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4"))
                .withServices(SNS, SQS);
        localstack.start();
        var dataSource = postgresDataSource();
        var jdbc = new JdbcTemplate(dataSource);
        stateJdbc = jdbc;
        jdbc.execute("DROP TABLE IF EXISTS prefab_stream_store, prefab_stream_inbox, "
                + "prefab_stream_outbox, prefab_stream_lease");
        PostgresStreamSchema.initialize(jdbc);
        var repository = new PostgresStreamStateRepository(
                jdbc, new DataSourceTransactionManager(dataSource), new JsonStreamStateCodec());

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
        var region = Region.of(localstack.getRegion());
        var snsClient = SnsClient.builder().endpointOverride(localstack.getEndpointOverride(SNS))
                .credentialsProvider(credentials).region(region).build();
        var sqsClient = SqsAsyncClient.builder().endpointOverride(localstack.getEndpointOverride(SQS))
                .credentialsProvider(credentials).region(region).build();

        queueClient = sqsClient;
        registry = new EventRegistry();
        // Sales are totalled per category, so they are ordered per category: one FIFO message group per category.
        registry.register("it-sales", Sale.class, Event.Serialization.JSON, Sale::category);
        registry.register("it-refunds", Refund.class, Event.Serialization.JSON, Refund::saleId);
        registry.register("it-products", Product.class, Event.Serialization.JSON, Product::category);
        registry.register("it-category-totals", CategoryTotal.class, Event.Serialization.JSON,
                CategoryTotal::category);
        registry.register("it-refunded-sales", RefundedSale.class, Event.Serialization.JSON, RefundedSale::saleId);

        var jsonUtil = new JsonUtil(JsonMapper.builder().findAndAddModules().build());
        var conversion = new DefaultConversionService();
        sqsUtil = new SqsUtil("it", "", 3, 100, 500, 1.5, snsClient, sqsClient,
                new SqsDeserializer(jsonUtil, registry, conversion), registry);
        snsTemplate = new SnsTemplate(snsClient);
        var serializer = new SnsSerializer(jsonUtil, registry, conversion);
        sink = new SnsSqsStreamSink(snsTemplate, sqsUtil, serializer, registry);
        var source = new SnsSqsStreamSource(sqsUtil, registry, "it", Duration.ofSeconds(60));
        var streams = new InProcessPrefabStreams(source, sink, repository, new SnsSqsTopicResolver(registry),
                TracingConfig.withTracing(), Duration.ofDays(1));

        var sales = streams.from(Sale.class);
        sales.groupBy(String.class, Sale::category)
                .aggregate(CategoryTotal.class, () -> new CategoryTotal(null, 0),
                        (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                .to(CategoryTotal.class);
        sales.groupBy(String.class, Sale::category)
                .windowedAggregate(AggregationWindow.tumbling(Duration.ofMinutes(1), Duration.ZERO),
                        CategoryTotal.class, () -> new CategoryTotal(null, 0),
                        (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                .to("it-windowed-totals.fifo");
        sales.join(streams.from(Refund.class), JoinWindow.of(Duration.ofMinutes(5), Duration.ZERO),
                        (sale, refund) -> new RefundedSale(sale.saleId(), sale.category(), refund.reason()))
                .to(RefundedSale.class);
        products = streams.globalStore(Product.class);
        topology = streams.start();

        listen("it-category-totals", CategoryTotal.class, totals);
        listen("it-windowed-totals.fifo", CategoryTotal.class, windowedTotals);
        listen("it-refunded-sales", RefundedSale.class, refunded);
    }

    @AfterAll
    static void stopInfrastructure() {
        if (topology != null) {
            topology.close();
        }
        sqsUtil.destroy();
        localstack.stop();
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    void streamsRunOnFifoTopicsWithStateInPostgres() {
        var now = Instant.now();

        publish("it-sales", "books", new Sale("s-1", "books", 5), now);
        publish("it-sales", "games", new Sale("s-2", "games", 7), now);
        publish("it-sales", "books", new Sale("s-3", "books", 10), now);
        publish("it-refunds", "s-1", new Refund("s-1", "damaged"), now.plusSeconds(1));
        publish("it-products", "books", new Product("books", "Books"), now);

        await("all running totals", () -> totals.containsAll(List.of(
                new CategoryTotal("books", 5), new CategoryTotal("games", 7), new CategoryTotal("books", 15))));
        assertThat(totals.stream().filter(total -> total.category().equals("books")))
                .containsExactly(new CategoryTotal("books", 5), new CategoryTotal("books", 15));
        await("three windowed totals", () -> windowedTotals.size() >= 3);
        await("refunded sale s-1", () -> refunded.contains(new RefundedSale("s-1", "books", "damaged")));
        await("product books in the global store", () -> products.get("books").isPresent());
        assertThat(products.get("books")).contains(new Product("books", "Books"));
    }

    @Test
    void aRedeliveredMessageDoesNotChangeStateOrOutputTwice() {
        var messageId = UUID.randomUUID().toString();
        var sale = new Sale("s-redelivered", "music", 3);

        // Two different FIFO deduplication IDs make SNS accept both, as when SQS delivers a message again.
        publishWithDeduplication("it-sales", sale, messageId, UUID.randomUUID().toString());
        publishWithDeduplication("it-sales", sale, messageId, UUID.randomUUID().toString());
        publish("it-sales", "music", new Sale("s-after", "music", 4), Instant.now());

        await("music total of 7", () -> totals.contains(new CategoryTotal("music", 7)));
        assertThat(totals.stream().filter(total -> total.category().equals("music")))
                .containsExactly(new CategoryTotal("music", 3), new CategoryTotal("music", 7));
    }

    private static void publish(String topic, String key, Object value, Instant eventTime) {
        sink.publish(topic + ".fifo", new StreamMessage(UUID.randomUUID().toString(), key, value, Map.of(), eventTime));
    }

    private static void publishWithDeduplication(String topic, Sale sale, String messageId, String deduplicationId) {
        var arn = sqsUtil.ensureTopicExists(topic, true);
        snsTemplate.sendNotification(arn, SnsNotification.builder(jsonOf(sale))
                .header("prefab-message-id", messageId)
                .groupId(sale.category())
                .deduplicationId(deduplicationId)
                .build());
    }

    private static String jsonOf(Object value) {
        return JsonMapper.builder().findAndAddModules().build().writeValueAsString(value);
    }

    private static <T> void listen(String topic, Class<T> type, List<T> into) {
        sqsUtil.subscribe(new SqsSubscriptionRequest<>(topic, "it-listener-" + topic, type, into::add)
                .withFifo(true));
    }

    private static void await(String what, Supplier<Boolean> condition) {
        var deadline = Instant.now().plus(Duration.ofSeconds(90));
        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for " + what, e);
            }
        }
        throw new AssertionError("Not seen within 90 seconds: %s%nState of the streams:%n%s"
                .formatted(what, snapshot()));
    }

    /** Describes where messages are, so a timeout says what the streams were doing. */
    private static String snapshot() {
        var snapshot = new StringBuilder();
        snapshot.append("inbox per source: ").append(stateJdbc.queryForList(
                "SELECT subscription, count(*) AS messages FROM prefab_stream_inbox GROUP BY subscription"))
                .append(System.lineSeparator());
        snapshot.append("outbox per topic: ").append(stateJdbc.queryForList(
                "SELECT topic, count(*) AS rows FROM prefab_stream_outbox GROUP BY topic"))
                .append(System.lineSeparator());
        snapshot.append("join state: ").append(stateJdbc.queryForList(
                "SELECT namespace, convert_from(state_value, 'UTF8') AS state FROM prefab_stream_store "
                        + "WHERE namespace LIKE '%join%'")).append(System.lineSeparator());
        snapshot.append("queues: ").append(queueDepths()).append(System.lineSeparator());
        snapshot.append("received: totals=").append(totals).append(" windowed=").append(windowedTotals)
                .append(" refunded=").append(refunded);
        return snapshot.toString();
    }

    private static Map<String, Map<String, String>> queueDepths() {
        var depths = new TreeMap<String, Map<String, String>>();
        try {
            for (var url : queueClient.listQueues().get().queueUrls()) {
                var attributes = queueClient.getQueueAttributes(builder -> builder.queueUrl(url).attributeNames(
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)).get().attributesAsStrings();
                depths.put(url.substring(url.lastIndexOf('/') + 1), attributes);
            }
        } catch (Exception e) {
            depths.put("unavailable", Map.of("reason", String.valueOf(e)));
        }
        return depths;
    }

    private static DataSource postgresDataSource() {
        var source = new PGSimpleDataSource();
        var externalUrl = System.getenv("PREFAB_TEST_POSTGRES_URL");
        if (externalUrl != null) {
            source.setUrl(externalUrl);
            source.setUser(System.getenv("PREFAB_TEST_POSTGRES_USER"));
            source.setPassword(System.getenv("PREFAB_TEST_POSTGRES_PASSWORD"));
            return source;
        }
        if (postgres == null) {
            postgres = new PostgreSQLContainer("postgres:16-alpine");
            postgres.start();
        }
        source.setUrl(postgres.getJdbcUrl());
        source.setUser(postgres.getUsername());
        source.setPassword(postgres.getPassword());
        return source;
    }
}
