package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaPrefabStreamsTopologyTest {

    @Test
    void fromClassToClass_shouldForwardRecordUsingDynamicSerde() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("orders.out", ProcessedOrder.class);

        var streamsBuilder = new TrackingStreamsBuilder();
        var streams = test.streams(streamsBuilder);
        var topology = streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), order.customer()))
                .to(ProcessedOrder.class);
        assertThat(streamsBuilder.buildInvocations()).isZero();

        assertThat(topology.buildTopology().describe().toString()).contains("orders.in").contains("orders.out");
        assertThat(streamsBuilder.buildInvocations()).isEqualTo(1);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("orders.in");
            var outputTopic = topologyTest.output("orders.out");

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));

            var forwarded = outputTopic.readValue();
            assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
            assertThat((ProcessedOrder) forwarded).isEqualTo(new ProcessedOrder("o-1", "Alice"));
        }
    }

    @Test
    void fromClassToTopic_shouldForwardRecordUsingExplicitTopic() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);

        var streamsBuilder = new TrackingStreamsBuilder();
        var streams = test.streams(streamsBuilder);
        var topology = streams.from(IncomingOrder.class).to("orders.dead-letter");
        assertThat(streamsBuilder.buildInvocations()).isZero();

        assertThat(topology.buildTopology().describe().toString()).contains("orders.in").contains("orders.dead-letter");
        assertThat(streamsBuilder.buildInvocations()).isEqualTo(1);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("orders.in");
            var outputTopic = topologyTest.rawOutput("orders.dead-letter");

            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));

            assertThat(outputTopic.readValue()).isNotEmpty();
        }
    }

    @Test
    void fromClass_shouldFailFastWhenNoTopicRegistered() {
        var streams = KafkaTopologyTestBootstrap.bootstrap().streams(new StreamsBuilder());

        assertThatThrownBy(() -> streams.from(IncomingOrder.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No topic registered for type");
    }

    @Test
    void fromClass_shouldFailFastWhenMultipleTopicsRegistered() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in.a", IncomingOrder.class);
        test.registerJson("orders.in.b", IncomingOrder.class);

        var streams = test.streams(new StreamsBuilder());

        assertThatThrownBy(() -> streams.from(IncomingOrder.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple topics registered for type");
    }

    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);

        var streams = test.streams(new StreamsBuilder());
        var topology = streams.from(IncomingOrder.class)
                .filter(order -> order.customer().startsWith("A"))
                .to("orders.filtered");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("orders.in");
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
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("orders.out", ProcessedOrder.class);

        var streams = test.streams(new StreamsBuilder());
        var topology = streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), order.customer().toUpperCase()))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("orders.in");
            var outputTopic = topologyTest.output("orders.out");

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));

            var forwarded = outputTopic.readValue();
            assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
            assertThat((ProcessedOrder) forwarded).isEqualTo(new ProcessedOrder("o-1", "ALICE"));
        }
    }

    @Test
    void flatMap_shouldExpandOneRecordToMany() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("words.in", WordBatch.class);

        var streams = test.streams(new StreamsBuilder());
        var topology = streams.from(WordBatch.class)
                .flatMap(batch -> List.of(batch.words().split(",")))
                .to("words.out");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("words.in");
            var outputTopic = topologyTest.rawOutput("words.out");

            inputTopic.pipeInput("b-1", new WordBatch("b-1", "hello,world,foo"));

            assertThat(outputTopic.readValuesToList()).hasSize(3);
        }
    }

    @Test
    void join_shouldEmitResultWhenKeysMatchWithinWindow() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("shipments.in", ShippingUpdate.class);
        test.registerJson("orders.joined", JoinedOrder.class);

        var streams = test.streams(new StreamsBuilder());
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input("orders.in");
            var shipmentsInputTopic = topologyTest.input("shipments.in");
            var outputTopic = topologyTest.output("orders.joined");

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
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("shipments.in", ShippingUpdate.class);
        test.registerJson("orders.joined", JoinedOrder.class);

        var streams = test.streams(new StreamsBuilder());
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input("orders.in");
            var shipmentsInputTopic = topologyTest.input("shipments.in");
            var outputTopic = topologyTest.output("orders.joined");

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
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("shipments.in", ShippingUpdate.class);
        test.registerJson("orders.joined", JoinedOrder.class);

        var streams = test.streams(new StreamsBuilder());
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(2), Duration.ZERO),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input("orders.in");
            var shipmentsInputTopic = topologyTest.input("shipments.in");
            var outputTopic = topologyTest.output("orders.joined");

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
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("orders.out", ProcessedOrder.class);

        var streams = test.streams(new StreamsBuilder());

        var topology = streams.from(IncomingOrder.class)
                .breakout(new KafkaStreamBreakoutAdapter<>(
                        nativeStream -> nativeStream.selectKey((key, value) -> "native-" + value.orderId(), Named.as("native-key"))
                ))
                .map(order -> new ProcessedOrder(order.orderId(), order.customer().toUpperCase()))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("orders.in");
            var outputTopic = topologyTest.output("orders.out");

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
        test.registerJson("orders.in", IncomingOrder.class);

        var streams = test.streams(new StreamsBuilder());

        assertThatThrownBy(() -> streams.from(IncomingOrder.class)
                .breakout(new NonKafkaBreakoutAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported breakout backend");
    }

    @Test
    void breakout_shouldFailFastWhenAdapterReturnsNonKStream() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);

        var streams = test.streams(new StreamsBuilder());

        assertThatThrownBy(() -> streams.from(IncomingOrder.class)
                .breakout(new InvalidKafkaReturnTypeAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must return a KStream");
    }

    @Test
    void multipleDefinitions_shouldShareCombinedTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("orders.audit.in", AuditOrder.class);

        var streams = test.streams(new StreamsBuilder());

        var forwardDefinition = streams.from(IncomingOrder.class).to("orders.out");
        var auditDefinition = streams.from(AuditOrder.class).to("orders.audit.out");

        var combinedTopologyDescription = forwardDefinition.buildTopology().describe().toString();

        assertThat(combinedTopologyDescription)
                .contains("orders.in")
                .contains("orders.out")
                .contains("orders.audit.in")
                .contains("orders.audit.out");
        assertThat(auditDefinition.nativeTopology().describe().toString()).isEqualTo(combinedTopologyDescription);
    }

    record IncomingOrder(String orderId, String customer) {
    }

    record ProcessedOrder(String orderId, String customer) {
    }

    record WordBatch(String batchId, String words) {
    }

    record AuditOrder(String orderId, String status) {
    }

    record ShippingUpdate(String orderId, String status) {
    }

    record JoinedOrder(String orderId, String customer, String shippingStatus) {
    }

    private static final class TrackingStreamsBuilder extends StreamsBuilder {
        private int buildInvocations;

        @Override
        public org.apache.kafka.streams.Topology build() {
            buildInvocations++;
            return super.build();
        }

        int buildInvocations() {
            return buildInvocations;
        }
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
}
