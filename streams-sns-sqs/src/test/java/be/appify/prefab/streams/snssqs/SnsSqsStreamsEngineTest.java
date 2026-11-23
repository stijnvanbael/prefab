package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.engine.InMemoryStreamStateRepository;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.snssqs.StreamFixtures.CategoryTotal;
import be.appify.prefab.streams.snssqs.StreamFixtures.Product;
import be.appify.prefab.streams.snssqs.StreamFixtures.Refund;
import be.appify.prefab.streams.snssqs.StreamFixtures.RefundedSale;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import be.appify.prefab.streams.tracing.TracingConfig;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the engine on the SNS/SQS source and sink, against an in-memory SNS/SQS. */
class SnsSqsStreamsEngineTest {
    private final FakeSnsSqs aws = new FakeSnsSqs();
    private final EventRegistry registry = new EventRegistry();
    private final StreamStateRepository repository = repository();
    private InProcessPrefabStreams streams;
    private InProcessPrefabStreams.RunningTopology topology;

    /** The state repository the engine runs on. */
    StreamStateRepository repository() {
        return new InMemoryStreamStateRepository();
    }

    @BeforeEach
    void registerEvents() {
        registry.register("sales.fifo", Sale.class, Event.Serialization.JSON, Sale::saleId);
        registry.register("category-totals.fifo", CategoryTotal.class, Event.Serialization.JSON,
                CategoryTotal::category);
        registry.register("products", Product.class, Event.Serialization.JSON);
        registry.register("refunds", Refund.class, Event.Serialization.JSON);
        var source = new SnsSqsStreamSource(aws.sqsUtil(), registry, "shop", Duration.ofMinutes(2));
        var sink = new SnsSqsStreamSink(aws.snsTemplate(), aws.sqsUtil(), aws.serializer(), registry);
        streams = new InProcessPrefabStreams(source, sink, repository, new SnsSqsTopicResolver(registry),
                TracingConfig.withTracing(), Duration.ofDays(7));
    }

    @AfterEach
    void stop() {
        if (topology != null) {
            topology.close();
        }
    }

    @Test
    void groupByAggregateKeepsARunningTotalPerCategory() {
        totalPerCategory();
        topology = streams.start();

        sale("s-1", "books", 5);
        sale("s-2", "games", 7);
        sale("s-3", "books", 10);
        aws.drain();

        assertThat(aws.payloads("category-totals.fifo", CategoryTotal.class))
                .containsExactly(new CategoryTotal("books", 5), new CategoryTotal("games", 7),
                        new CategoryTotal("books", 15));
    }

    @Test
    void outputIsPublishedOnAFifoTopicWithTheKeyAsGroupAndTheMessageIdForDeduplication() {
        totalPerCategory();
        topology = streams.start();

        sale("s-1", "books", 5);
        aws.drain();

        var published = aws.publications("category-totals.fifo").getFirst();
        assertThat(published.groupId()).isEqualTo("books");
        assertThat(published.deduplicationId()).isEqualTo(published.attributes().get("prefab-message-id"));
        assertThat(published.attributes()).containsKeys("prefab-event-time", "prefab-correlation-ids");
    }

    @Test
    void aRedeliveredMessageChangesNeitherStateNorOutput() {
        totalPerCategory();
        topology = streams.start();
        sale("s-1", "books", 5);
        sale("s-2", "books", 10);
        aws.drain();
        var before = aws.payloads("category-totals.fifo", CategoryTotal.class);

        aws.redeliverAll();
        aws.drain();

        assertThat(aws.payloads("category-totals.fifo", CategoryTotal.class)).isEqualTo(before);
        sale("s-3", "books", 1);
        aws.drain();
        assertThat(aws.payloads("category-totals.fifo", CategoryTotal.class).getLast())
                .isEqualTo(new CategoryTotal("books", 16));
    }

    @Test
    void messagesAreDeletedOnlyAfterTheyWereProcessed() {
        totalPerCategory();
        topology = streams.start();

        sale("s-1", "books", 5);
        aws.drain();

        assertThat(aws.deletedMessages()).hasSize(1);
    }

    @Test
    void aFailingMessageIsNotDeletedSoSqsCanRedeliverIt() {
        streams.from(Sale.class)
                .map(SnsSqsStreamsEngineTest::explode)
                .to(CategoryTotal.class);
        topology = streams.start();
        sale("s-1", "books", 5);

        assertThatThrownBy(aws::drain).hasMessageContaining("boom");
        assertThat(aws.deletedMessages()).isEmpty();
    }

    @Test
    void windowedAggregateUsesTheEventTimeOfTheMessage() {
        streams.from(Sale.class)
                .groupBy(String.class, Sale::category)
                .windowedAggregate(AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO),
                        CategoryTotal.class, () -> new CategoryTotal(null, 0),
                        (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                .to(CategoryTotal.class);
        topology = streams.start();

        saleAt("s-1", "books", 1, "2026-01-01T10:00:01Z");
        saleAt("s-2", "books", 2, "2026-01-01T10:00:04Z");
        saleAt("s-3", "books", 4, "2026-01-01T10:00:11Z");
        saleAt("s-4", "books", 8, "2026-01-01T10:00:02Z");
        aws.drain();

        assertThat(aws.payloads("category-totals.fifo", CategoryTotal.class))
                .containsExactly(new CategoryTotal("books", 1), new CategoryTotal("books", 3),
                        new CategoryTotal("books", 4));
    }

    @Test
    void eventTimeFallsBackToTheSentTimestampWhenTheMessageHasNone() {
        streams.from(Sale.class)
                .groupBy(String.class, Sale::category)
                .windowedAggregate(AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO),
                        CategoryTotal.class, () -> new CategoryTotal(null, 0),
                        (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                .to(CategoryTotal.class);
        topology = streams.start();

        aws.publish("sales.fifo", new Sale("s-1", "books", 1), Map.of(), "s-1");
        aws.advance(Duration.ofSeconds(5));
        aws.publish("sales.fifo", new Sale("s-2", "books", 2), Map.of(), "s-2");
        aws.advance(Duration.ofSeconds(30));
        aws.publish("sales.fifo", new Sale("s-3", "books", 4), Map.of(), "s-3");
        aws.drain();

        assertThat(aws.payloads("category-totals.fifo", CategoryTotal.class))
                .extracting(CategoryTotal::total).containsExactly(1, 3, 4);
    }

    @Test
    void joinMatchesRecordsOfTwoSourcesWithinTheWindow() {
        streams.from(Sale.class)
                .join(streams.from(Refund.class), JoinWindow.of(Duration.ofSeconds(30), Duration.ZERO),
                        (sale, refund) -> new RefundedSale(sale.saleId(), sale.category(), refund.reason()))
                .to("refunded-sales");
        topology = streams.start();

        saleAt("s-1", "books", 3, "2026-01-01T10:00:00Z");
        aws.publish("refunds", new Refund("s-1", "damaged"), Map.of("prefab-event-time", "2026-01-01T10:00:10Z"),
                null);
        aws.publish("refunds", new Refund("s-2", "unknown sale"),
                Map.of("prefab-event-time", "2026-01-01T10:00:11Z"), null);
        aws.drain();

        assertThat(aws.payloads("refunded-sales", RefundedSale.class))
                .containsExactly(new RefundedSale("s-1", "books", "damaged"));
    }

    @Test
    void globalStoreIsFilledFromItsOwnQueueAndEmptiedByATombstone() {
        GlobalStore<String, Product> products = streams.globalStore(Product.class);
        topology = streams.start();

        aws.publish("products", new Product("books", "Books"), Map.of(), null);
        aws.drain();
        assertThat(products.get("books")).contains(new Product("books", "Books"));

        aws.publishTombstone("products", "books", null);
        aws.drain();
        assertThat(products.get("books")).isEmpty();
    }

    @Test
    void everySourceAndGlobalStoreGetsItsOwnQueue() {
        totalPerCategory();
        streams.globalStore(Product.class);

        topology = streams.start();

        assertThat(aws.queueNames()).hasSize(2).allSatisfy(name -> assertThat(name)
                .startsWith("shop-streams-").hasSizeLessThanOrEqualTo(75));
        assertThat(aws.queueNames()).doesNotHaveDuplicates();
        assertThat(aws.visibilityTimeouts()).containsOnly(Duration.ofMinutes(2));
    }

    @Test
    void breakoutFailsAtStartup() {
        streams.from(Sale.class)
                .breakout(new KafkaOnlyBreakout())
                .end();

        assertThatThrownBy(streams::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("breakout");
    }

    private static CategoryTotal explode(Sale sale) {
        throw new IllegalStateException("boom for " + sale.saleId());
    }

    private void totalPerCategory() {
        streams.from(Sale.class)
                .groupBy(String.class, Sale::category)
                .aggregate(CategoryTotal.class, () -> new CategoryTotal(null, 0),
                        (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                .to(CategoryTotal.class);
    }

    private void sale(String id, String category, int amount) {
        aws.publish("sales.fifo", new Sale(id, category, amount), Map.of(), id);
    }

    private void saleAt(String id, String category, int amount, String eventTime) {
        aws.publish("sales.fifo", new Sale(id, category, amount), Map.of("prefab-event-time", eventTime), id);
    }

    private static final class KafkaOnlyBreakout
            implements StreamBreakoutAdapter<String, Sale, String, Sale, Object, Object> {
        @Override
        public StreamBackend backend() {
            return StreamBackend.named("kafka");
        }

        @Override
        public Class<Object> nativeInputType() {
            return Object.class;
        }

        @Override
        public Object apply(Object nativeStream) {
            return nativeStream;
        }
    }
}
