package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamRecord;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaPrefabStreamsTopologyTest {
    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .filter(order -> order.customer().startsWith("A"))
                .to("orders.filtered");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.rawOutput("orders.filtered");

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));
            inputTopic.pipeInput("o-3", new IncomingOrder("o-3", "Anna"));

            assertThat(outputTopic.readValuesToList()).hasSize(2);
        }
    }

    @Test
    void map_shouldTransformValues() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), order.customer().toUpperCase()))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.output(ProcessedOrder.class);

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));

            var forwarded = outputTopic.readValue();
            assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
            assertThat(forwarded).isEqualTo(new ProcessedOrder("o-1", "ALICE"));
        }
    }

    @Test
    void flatMap_shouldExpandOneRecordToMany() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(WordBatch.class)
                .flatMap(batch -> List.of(batch.words().split(",")))
                .to("words.out");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(WordBatch.class);
            var outputTopic = topologyTest.rawOutput("words.out");

            inputTopic.pipeInput("b-1", new WordBatch("b-1", "hello,world,foo"));

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
                    new IncomingOrder("o-1", "Alice"),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    "o-1",
                    new ShippingUpdate("o-1", "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValue())
                    .isEqualTo(new JoinedOrder("o-1", "Alice", "SHIPPED"));
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
                    new IncomingOrder("o-1", "Alice"),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    "o-2",
                    new ShippingUpdate("o-2", "SHIPPED"),
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
                    new IncomingOrder("o-1", "Alice"),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    "o-1",
                    new ShippingUpdate("o-1", "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValuesToList()).isEmpty();
        }
    }

    @Test
    void breakout_shouldApplyKafkaNativeFragmentWithinPrefabPipeline() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .breakout(new KafkaStreamBreakoutAdapter<>(
                        nativeStream -> nativeStream.selectKey((key, value) -> "native-" + value.orderId(), Named.as("native-key"))
                ))
                .map(order -> new ProcessedOrder(order.orderId(), order.customer().toUpperCase()))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.output(ProcessedOrder.class);

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "bob"));

            var output = outputTopic.readValuesToList();
            assertThat(output)
                    .containsExactlyInAnyOrder(
                            new ProcessedOrder("o-1", "ALICE"),
                            new ProcessedOrder("o-2", "BOB")
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

            input.pipeInput("o-1", new IncomingOrder("o-1", "alice"));
            input.pipeInput("o-2", new IncomingOrder("o-2", "bob"));
            input.pipeInput("o-3", new IncomingOrder("o-3", "alice"));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new OrderCount("alice", 1),
                            new OrderCount("bob", 1),
                            new OrderCount("alice", 2)
                    );
        }
    }

    @Test
    void multipleStreams_shouldBeJoinedInOneTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), order.customer().toUpperCase()))
                .to(ProcessedOrder.class);

        var topology = streams.from(ProcessedOrder.class)
                .map(order -> new ShippingUpdate(order.orderId(), "PENDING"))
                .to(ShippingUpdate.class);

        try(var topologyTest = test.run(topology)) {
            var input = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.output(ShippingUpdate.class);

            input.pipeInput("o-1", new IncomingOrder("o-1", "alice"));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new ShippingUpdate("o-1", "PENDING")
                    );
        }
    }

    record IncomingOrder(String orderId, String customer) {
    }

    record ProcessedOrder(String orderId, String customer) {
    }

    record WordBatch(String batchId, String words) {
    }

    record ShippingUpdate(String orderId, String status) {
    }

    record JoinedOrder(String orderId, String customer, String shippingStatus) {
    }

    record OrderCount(String customer, int numberOfOrders) {
    }

    private static final class NonKafkaBreakoutAdapter
            implements StreamBreakoutAdapter<IncomingOrder, IncomingOrder, KStream<String, IncomingOrder>, KStream<String, IncomingOrder>> {

        @Override
        public StreamBackend backend() {
            return StreamBackend.CUSTOM;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<KStream<String, IncomingOrder>> nativeInputType() {
            return (Class<KStream<String, IncomingOrder>>) (Class<?>) KStream.class;
        }

        @Override
        public KStream<String, IncomingOrder> apply(KStream<String, IncomingOrder> nativeStream) {
            return nativeStream;
        }
    }

    private static final class InvalidKafkaReturnTypeAdapter
            implements StreamBreakoutAdapter<IncomingOrder, IncomingOrder, KStream<String, IncomingOrder>, Object> {

        @Override
        public StreamBackend backend() {
            return StreamBackend.KAFKA;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<KStream<String, IncomingOrder>> nativeInputType() {
            return (Class<KStream<String, IncomingOrder>>) (Class<?>) KStream.class;
        }

        @Override
        public Object apply(KStream<String, IncomingOrder> nativeStream) {
            return "not-a-stream";
        }
    }

    private static class CountOrderProcessor extends StatefulStreamProcessor<IncomingOrder, OrderCount> {
        public CountOrderProcessor() {
            super(OrderCount.class);
        }

        @Override
        public void process(StreamRecord<IncomingOrder> streamRecord) {
            var orderCount = store(OrderCount.class).putOrUpdate(streamRecord.value().customer(),
                    () -> new OrderCount(streamRecord.value().customer(), 1),
                    existing -> new OrderCount(existing.customer(), existing.numberOfOrders() + 1));
            forward(streamRecord.key(), orderCount);
        }
    }
}
