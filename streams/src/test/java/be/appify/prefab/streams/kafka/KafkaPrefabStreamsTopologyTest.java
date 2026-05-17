package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.core.util.SerializationRegistry;
import be.appify.prefab.streams.PrefabStream;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Properties;

class KafkaPrefabStreamsTopologyTest {

    @Test
    void fromClassToClass_shouldForwardRecordUsingDynamicSerde() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.out", ProcessedOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);
        fixture.serializationRegistry.register("orders.out", Event.Serialization.JSON);

        var streamsBuilder = new TrackingStreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );
        var topology = streams.from(IncomingOrder.class).to(ProcessedOrder.class);
        assertThat(streamsBuilder.buildInvocations()).isZero();

        assertThat(topology.buildTopology().describe().toString()).contains("orders.in").contains("orders.out");
        assertThat(streamsBuilder.buildInvocations()).isEqualTo(1);

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.out", new StringDeserializer(), fixture.deserializer);

            var order = new IncomingOrder("o-1", "Alice");
            inputTopic.pipeInput("o-1", order);

            var forwarded = outputTopic.readValue();
            assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
            assertThat((ProcessedOrder) forwarded).isEqualTo(new ProcessedOrder("o-1", "Alice"));
        }
    }

    @Test
    void fromClassToTopic_shouldForwardRecordUsingExplicitTopic() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);

        var streamsBuilder = new TrackingStreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );
        var topology = streams.from(IncomingOrder.class).to("orders.dead-letter");
        assertThat(streamsBuilder.buildInvocations()).isZero();

        assertThat(topology.buildTopology().describe().toString()).contains("orders.in").contains("orders.dead-letter");
        assertThat(streamsBuilder.buildInvocations()).isEqualTo(1);

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.dead-letter", new StringDeserializer(),
                    new ByteArrayDeserializer());

            var order = new IncomingOrder("o-2", "Bob");
            inputTopic.pipeInput("o-2", order);

            assertThat(outputTopic.readValue()).isNotEmpty();
        }
    }

    @Test
    void fromClass_shouldFailFastWhenNoTopicRegistered() {
        var fixture = fixture();

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        assertThatThrownBy(() -> streams.from(IncomingOrder.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No topic registered for type");
    }

    @Test
    void fromClass_shouldFailFastWhenMultipleTopicsRegistered() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in.a", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.in.b", IncomingOrder.class);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        assertThatThrownBy(() -> streams.from(IncomingOrder.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple topics registered for type");
    }

    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );
        var topology = streams.from(IncomingOrder.class)
                .filter(order -> order.customer().startsWith("A"))
                .to("orders.filtered");

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.filtered", new StringDeserializer(),
                    new ByteArrayDeserializer());

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));
            inputTopic.pipeInput("o-3", new IncomingOrder("o-3", "Anna"));

            var output = outputTopic.readValuesToList();
            assertThat(output).hasSize(2);
        }
    }

    @Test
    void map_shouldTransformValues() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.out", ProcessedOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);
        fixture.serializationRegistry.register("orders.out", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );
        var topology = streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), order.customer().toUpperCase()))
                .to(ProcessedOrder.class);

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.out", new StringDeserializer(), fixture.deserializer);

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));

            var forwarded = outputTopic.readValue();
            assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
            assertThat((ProcessedOrder) forwarded).isEqualTo(new ProcessedOrder("o-1", "ALICE"));
        }
    }

    @Test
    void flatMap_shouldExpandOneRecordToMany() {
        var fixture = fixture();

        fixture.typeResolver.registerType("words.in", WordBatch.class);
        fixture.serializationRegistry.register("words.in", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );
        var topology = streams.from(WordBatch.class)
                .flatMap(batch -> List.of(batch.words().split(",")))
                .to("words.out");

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("words.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("words.out", new StringDeserializer(),
                    new ByteArrayDeserializer());

            inputTopic.pipeInput("b-1", new WordBatch("b-1", "hello,world,foo"));

            assertThat(outputTopic.readValuesToList()).hasSize(3);
        }
    }

    @Test
    void branch_shouldEmitOnlyRecordsMatchingSinglePredicate() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.out", ProcessedOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);
        fixture.serializationRegistry.register("orders.out", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        var topology = streams.from(IncomingOrder.class)
                .branch(order -> order.customer().startsWith("A"))
                .map(order -> new ProcessedOrder(order.orderId(), order.customer()))
                .to(ProcessedOrder.class);

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var aCustomersTopic = driver.createOutputTopic("orders.out", new StringDeserializer(),
                    fixture.deserializer);

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));
            inputTopic.pipeInput("o-3", new IncomingOrder("o-3", "Anna"));

            var aCustomers = aCustomersTopic.readValuesToList();
            assertThat(aCustomers)
                    .hasSize(2)
                    .allMatch(ProcessedOrder.class::isInstance);
            assertThat(aCustomers.stream().map(ProcessedOrder.class::cast).map(ProcessedOrder::customer).toList())
                    .containsExactlyInAnyOrder("Alice", "Anna");
        }
    }

    @Test
    void branchBySubtype_shouldFilterAndCastToSubtype() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.priority", PriorityCustomer.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);
        fixture.serializationRegistry.register("orders.priority", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        var topology = streams.from(IncomingOrder.class)
                .map(order -> order.customer().startsWith("A")
                        ? (CustomerTieredOrder) new PriorityCustomer(order.orderId(), order.customer())
                        : new StandardCustomer(order.orderId(), order.customer()))
                .branch(PriorityCustomer.class)
                .to("orders.priority");

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.priority", new StringDeserializer(), fixture.deserializer);

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));
            inputTopic.pipeInput("o-3", new IncomingOrder("o-3", "Anna"));

            var preferredCustomers = outputTopic.readValuesToList();
            assertThat(preferredCustomers)
                    .hasSize(2)
                    .allMatch(PriorityCustomer.class::isInstance);
            assertThat(preferredCustomers.stream().map(PriorityCustomer.class::cast).map(PriorityCustomer::customer).toList())
                    .containsExactlyInAnyOrder("Alice", "Anna");
        }
    }

    @Test
    void merge_shouldSupportCommonSupertypeAcrossSiblingStreams() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.segmented", SegmentedOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);
        fixture.serializationRegistry.register("orders.segmented", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        var classified = streams.from(IncomingOrder.class)
                .map(order -> order.customer().startsWith("A")
                        ? (CustomerTieredOrder) new PriorityCustomer(order.orderId(), order.customer())
                        : new StandardCustomer(order.orderId(), order.customer()));

        var priority = classified.branch(PriorityCustomer.class);
        var standard = classified.branch(StandardCustomer.class);

        PrefabStream<CustomerTieredOrder> merged = priority.merge(standard);

        var topology = merged
                .map(order -> new SegmentedOrder(order.orderId(), order.customer(), order.tier()))
                .to("orders.segmented");

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.segmented", new StringDeserializer(), fixture.deserializer);

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));

            var mergedOrders = outputTopic.readValuesToList();
            assertThat(mergedOrders)
                    .hasSize(2)
                    .allMatch(SegmentedOrder.class::isInstance);
            assertThat(mergedOrders.stream().map(SegmentedOrder.class::cast).map(SegmentedOrder::tier).toList())
                    .containsExactlyInAnyOrder("PRIORITY", "STANDARD");
        }
    }

    @Test
    void breakout_shouldApplyKafkaNativeFragmentWithinPrefabPipeline() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.out", ProcessedOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);
        fixture.serializationRegistry.register("orders.out", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        var topology = streams.from(IncomingOrder.class)
                .breakout(new KafkaStreamBreakoutAdapter<>(
                        nativeStream -> nativeStream.selectKey((key, value) -> "native-" + value.orderId(), Named.as("native-key"))
                ))
                .map(order -> new ProcessedOrder(order.orderId(), order.customer().toUpperCase()))
                .to(ProcessedOrder.class);

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.out", new StringDeserializer(), fixture.deserializer);

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
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);

        var streams = new KafkaPrefabStreams(
                new StreamsBuilder(),
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        assertThatThrownBy(() -> streams.from(IncomingOrder.class)
                .breakout(new NonKafkaBreakoutAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported breakout backend");
    }

    @Test
    void breakout_shouldFailFastWhenAdapterReturnsNonKStream() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);

        var streams = new KafkaPrefabStreams(
                new StreamsBuilder(),
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

        assertThatThrownBy(() -> streams.from(IncomingOrder.class)
                .breakout(new InvalidKafkaReturnTypeAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must return a KStream");
    }

    @Test
    void multipleDefinitions_shouldShareCombinedTopology() {
        var fixture = fixture();

        fixture.typeResolver.registerType("orders.in", IncomingOrder.class);
        fixture.typeResolver.registerType("orders.audit.in", AuditOrder.class);
        fixture.serializationRegistry.register("orders.in", Event.Serialization.JSON);
        fixture.serializationRegistry.register("orders.audit.in", Event.Serialization.JSON);

        var streamsBuilder = new StreamsBuilder();
        var streams = new KafkaPrefabStreams(
                streamsBuilder,
                new KafkaTopicResolver(fixture.typeResolver),
                fixture.serializer,
                fixture.deserializer
        );

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

    private static Fixture fixture() {
        var serializationRegistry = new SerializationRegistry();
        var typeResolver = new EventRegistry();
        var conversionService = new DefaultConversionService();
        var kafkaProperties = new KafkaProperties();
        var serializer = new DynamicSerializer(kafkaProperties, conversionService, serializationRegistry);
        var deserializer = new DynamicDeserializer(kafkaProperties, conversionService, serializationRegistry,
                typeResolver);
        return new Fixture(serializationRegistry, typeResolver, serializer, deserializer);
    }

    private static Properties streamsConfig() {
        var properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "prefab-streams-topology-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        return properties;
    }

    record IncomingOrder(String orderId, String customer) {
    }

    record ProcessedOrder(String orderId, String customer) {
    }

    record WordBatch(String batchId, String words) {
    }

    record AuditOrder(String orderId, String status) {
    }

    sealed interface CustomerTieredOrder permits PriorityCustomer, StandardCustomer {
        String orderId();

        String customer();

        String tier();
    }

    record PriorityCustomer(String orderId, String customer) implements CustomerTieredOrder {
        @Override
        public String tier() {
            return "PRIORITY";
        }
    }

    record StandardCustomer(String orderId, String customer) implements CustomerTieredOrder {
        @Override
        public String tier() {
            return "STANDARD";
        }
    }

    record SegmentedOrder(String orderId, String customer, String tier) {
    }

    private record Fixture(
            SerializationRegistry serializationRegistry,
            EventRegistry typeResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) {
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



