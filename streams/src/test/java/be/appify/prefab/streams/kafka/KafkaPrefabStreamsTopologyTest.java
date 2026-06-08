package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamRecord;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaPrefabStreamsTopologyTest {
    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .filter(order -> order.customer().id().startsWith("A"))
                .to("orders.filtered");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.rawOutput("orders.filtered");

            inputTopic.pipeInput("o-1", new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")));
            inputTopic.pipeInput("o-2", new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("Bob")));
            inputTopic.pipeInput("o-3", new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("Anna")));

            assertThat(outputTopic.readValuesToList()).hasSize(2);
        }
    }

    @Test
    void map_shouldTransformValues() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.output(ProcessedOrder.class);

            inputTopic.pipeInput("o-1", new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")));

            var forwarded = outputTopic.readValue();
            assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
            assertThat(forwarded).isEqualTo(new ProcessedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE")));
        }
    }

    @Test
    void flatMap_shouldExpandOneRecordToMany() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(WordBatch.class)
                .flatMap(batch -> Stream.of(batch.words().split(","))
                        .map(word -> new WordBatch(batch.batchId(), word))
                        .toList())
                .to("words.out");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(WordBatch.class);
            var outputTopic = topologyTest.rawOutput("words.out");

            inputTopic.pipeInput("b-1", new WordBatch(Reference.fromId("b-1"), "hello,world,foo"));

            assertThat(outputTopic.readValuesToList()).hasSize(3);
        }
    }

    @Test
    void join_shouldEmitResultWhenKeysMatchWithinWindow() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var streams = test.streams();
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input(IncomingOrder.class);
            var shipmentsInputTopic = topologyTest.input(ShippingUpdate.class);
            var outputTopic = topologyTest.output(JoinedOrder.class);

            ordersInputTopic.pipeInput(
                    "o-1",
                    new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    "o-1",
                    new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValue())
                    .isEqualTo(new JoinedOrder(Reference.fromId("o-1"), Reference.fromId("Alice"), "SHIPPED"));
        }
    }

    @Test
    void join_shouldNotEmitResultWhenKeysDoNotMatch() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var streams = test.streams();
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input(IncomingOrder.class);
            var shipmentsInputTopic = topologyTest.input(ShippingUpdate.class);
            var outputTopic = topologyTest.output(JoinedOrder.class);

            ordersInputTopic.pipeInput(
                    "o-1",
                    new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    "o-2",
                    new ShippingUpdate(Reference.fromId("o-2"), "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValuesToList()).isEmpty();
        }
    }

    @Test
    void join_shouldNotEmitResultWhenRecordsAreOutsideWindow() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var streams = test.streams();
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(2), Duration.ZERO),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input(IncomingOrder.class);
            var shipmentsInputTopic = topologyTest.input(ShippingUpdate.class);
            var outputTopic = topologyTest.output(JoinedOrder.class);

            ordersInputTopic.pipeInput(
                    "o-1",
                    new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    "o-1",
                    new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValuesToList()).isEmpty();
        }
    }

    @Test
    void breakout_shouldApplyKafkaNativeFragmentWithinPrefabPipeline() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .breakout(new KafkaStreamBreakoutAdapter<Reference<Order>, Reference<Order>, IncomingOrder, IncomingOrder>(
                        nativeStream -> nativeStream.selectKey(
                                (key, value) -> Reference.fromId("native-" + value.orderId().id()),
                                Named.as("native-key")
                        )
                ))
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.output(ProcessedOrder.class);

            inputTopic.pipeInput("o-1", new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            inputTopic.pipeInput("o-2", new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));

            var output = outputTopic.readValuesToList();
            assertThat(output)
                    .containsExactlyInAnyOrder(
                            new ProcessedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE")),
                            new ProcessedOrder(Reference.fromId("o-2"), Reference.fromId("BOB"))
                    );
        }
    }

    @Test
    void breakout_shouldFailFastWhenAdapterTargetsDifferentBackend() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        assertThatThrownBy(() -> test.streams().from(IncomingOrder.class)
                .breakout(new NonKafkaBreakoutAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported breakout backend");
    }

    @Test
    void breakout_shouldFailFastWhenAdapterReturnsNonKStream() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        assertThatThrownBy(() -> test.streams().from(IncomingOrder.class)
                .breakout(new InvalidKafkaReturnTypeAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must return a KStream");
    }

    @Test
    void process_shouldPerformStatefulOperations() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var countDefinition = test.streams().from(IncomingOrder.class)
                .process(new CountOrderProcessor())
                .to("prefab-streams-test-order-count");

        try (var topologyTest = test.run(countDefinition)) {
            var input = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.output(OrderCount.class);

            input.pipeInput("o-1", new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            input.pipeInput("o-2", new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));
            input.pipeInput("o-3", new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("alice")));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new OrderCount(Reference.fromId("alice"), 1),
                            new OrderCount(Reference.fromId("bob"), 1),
                            new OrderCount(Reference.fromId("alice"), 2)
                    );
        }
    }

    @Test
    void multipleStreams_shouldBeJoinedInOneTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        var topology = streams.from(ProcessedOrder.class)
                .map(order -> new ShippingUpdate(order.orderId(), "PENDING"))
                .to(ShippingUpdate.class);

        try (var topologyTest = test.run(topology)) {
            var input = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.output(ShippingUpdate.class);

            input.pipeInput("o-1", new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new ShippingUpdate(Reference.fromId("o-1"), "PENDING")
                    );
        }
    }

    @Test
    void joinAfterMap_shouldUseTheRightSerdes() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        var topology = streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(2), Duration.ZERO),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputOrder = topologyTest.input(IncomingOrder.class);
            var inputShippingUpdate = topologyTest.input(ShippingUpdate.class);
            var output = topologyTest.output(JoinedOrder.class);

            inputOrder.pipeInput("o-1", new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            inputShippingUpdate.pipeInput("o-1", new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new JoinedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE"), "SHIPPED")
                    );
        }
    }

    @Test
    void mergeAfterMap_shouldUseTheRightSerdes() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        var orders = streams.from(IncomingOrder.class)
                .map(order -> new OrderEvent.OrderCreated(order.orderId(), order.customer()));
        var shippingUpdates = streams.from(ShippingUpdate.class)
                .filter(update -> "SHIPPED".equals(update.status()))
                .map(update -> new OrderEvent.OrderShipped(update.orderId()));
        var topology = streams.merge(orders, shippingUpdates)
                .to(OrderEvent.class);

        try (var topologyTest = test.run(topology)) {
            var inputOrder = topologyTest.input(IncomingOrder.class);
            var inputShippingUpdate = topologyTest.input(ShippingUpdate.class);
            var output = topologyTest.output(OrderEvent.class);

            inputOrder.pipeInput("o-1", new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            inputShippingUpdate.pipeInput("o-1", new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new OrderEvent.OrderCreated(Reference.fromId("o-1"), Reference.fromId("alice")),
                            new OrderEvent.OrderShipped(Reference.fromId("o-1"))
                    );
        }
    }

    record IncomingOrder(Reference<Order> orderId, Reference<Customer> customer) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record ProcessedOrder(Reference<Order> orderId, Reference<Customer> customer) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record Order() {
    }

    record Customer() {
    }

    record WordBatch(Reference<Batch> batchId, String words) implements Keyed<Reference<Batch>> {
        @Override
        public Reference<Batch> key() {
            return batchId;
        }
    }

    record Batch() {
    }

    record ShippingUpdate(Reference<Order> orderId, String status) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record JoinedOrder(Reference<Order> orderId, Reference<Customer> customer,
                       String shippingStatus) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record OrderCount(Reference<Customer> customer, int numberOfOrders) implements Keyed<Reference<Customer>> {
        @Override
        public Reference<Customer> key() {
            return customer;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    sealed interface OrderEvent extends Keyed<Reference<Order>> permits OrderEvent.OrderCreated, OrderEvent.OrderShipped {
        @Override
        default Reference<Order> key() {
            return orderId();
        }

        Reference<Order> orderId();

        record OrderCreated(Reference<Order> orderId, Reference<Customer> customer) implements OrderEvent {}

        record OrderShipped(Reference<Order> orderId) implements OrderEvent {}
    }



    private static final class NonKafkaBreakoutAdapter
            implements StreamBreakoutAdapter<Reference<Order>, IncomingOrder, Reference<Order>, IncomingOrder,
            KStream<Reference<Order>, IncomingOrder>, KStream<Reference<Order>, IncomingOrder>> {

        @Override
        public StreamBackend backend() {
            return StreamBackend.CUSTOM;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<KStream<Reference<Order>, IncomingOrder>> nativeInputType() {
            return (Class<KStream<Reference<Order>, IncomingOrder>>) (Class<?>) KStream.class;
        }

        @Override
        public KStream<Reference<Order>, IncomingOrder> apply(KStream<Reference<Order>, IncomingOrder> nativeStream) {
            return nativeStream;
        }
    }

    private static final class InvalidKafkaReturnTypeAdapter
            implements StreamBreakoutAdapter<Reference<Order>, IncomingOrder, Reference<Order>, IncomingOrder,
            KStream<Reference<Order>, IncomingOrder>, Object> {

        @Override
        public StreamBackend backend() {
            return StreamBackend.KAFKA;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<KStream<Reference<Order>, IncomingOrder>> nativeInputType() {
            return (Class<KStream<Reference<Order>, IncomingOrder>>) (Class<?>) KStream.class;
        }

        @Override
        public Object apply(KStream<Reference<Order>, IncomingOrder> nativeStream) {
            return "not-a-stream";
        }
    }

    private static class CountOrderProcessor
            extends StatefulStreamProcessor<Reference<Order>, IncomingOrder, Reference<Customer>, OrderCount> {
        public CountOrderProcessor() {
            super(OrderCount.class);
        }

        @Override
        public void process(StreamRecord<Reference<Order>, IncomingOrder> streamRecord) {
            var orderCount = store(OrderCount.class).putOrUpdate(
                    streamRecord.value().customer(),
                    () -> new OrderCount(streamRecord.value().customer(), 1),
                    existing -> new OrderCount(existing.customer(), existing.numberOfOrders() + 1));
            forward(orderCount.key(), orderCount);
        }
    }
}
