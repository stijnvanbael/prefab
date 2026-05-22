package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
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
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams(new StreamsBuilder());

        assertThatThrownBy(() -> streams.from(IncomingOrder.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No topic registered for type");
    }

    @Test
    void fromClass_shouldFailFastWhenMultipleTopicsRegistered() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerType("orders.in.a", IncomingOrder.class);
        test.registerType("orders.in.b", IncomingOrder.class);

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
    void branch_shouldRouteRecordsToMatchingOutputStreams() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);

        var streams = test.streams(new StreamsBuilder());

        var branches = streams.from(IncomingOrder.class)
                .branch(
                        order -> order.customer().startsWith("A"),
                        order -> true
                );
        branches.get(0).to("orders.a-customers");
        var topology = branches.get(1).to("orders.other-customers");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("orders.in");
            var aCustomersTopic = topologyTest.rawOutput("orders.a-customers");
            var otherCustomersTopic = topologyTest.rawOutput("orders.other-customers");

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));
            inputTopic.pipeInput("o-3", new IncomingOrder("o-3", "Anna"));

            assertThat(aCustomersTopic.readValuesToList()).hasSize(2);
            assertThat(otherCustomersTopic.readValuesToList()).hasSize(1);
        }
    }

    @Test
    void merge_shouldCombineRecordsFromTwoBranchesIntoSingleOutput() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        test.registerJson("orders.in", IncomingOrder.class);
        test.registerJson("orders.out", ProcessedOrder.class);

        var streams = test.streams(new StreamsBuilder());

        var branches = streams.from(IncomingOrder.class)
                .branch(
                        order -> order.customer().startsWith("A"),
                        order -> true
                );

        var topology = branches.get(0)
                .map(order -> new ProcessedOrder(order.orderId(), "A:" + order.customer()))
                .merge(branches.get(1).map(order -> new ProcessedOrder(order.orderId(), "B:" + order.customer())))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input("orders.in");
            var outputTopic = topologyTest.output("orders.out");

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));

            var merged = outputTopic.readValuesToList();
            assertThat(merged)
                    .hasSize(2)
                    .allMatch(ProcessedOrder.class::isInstance);
            assertThat(merged.stream().map(ProcessedOrder.class::cast).map(ProcessedOrder::customer).toList())
                    .containsExactlyInAnyOrder("A:Alice", "B:Bob");
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
                        nativeStream -> nativeStream.selectKey((key, value) -> "native-" + value.orderId(),
                                Named.as("native-key"))
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



