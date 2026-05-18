package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.util.SerializationRegistry;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
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

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "alice"));

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
    void branch_shouldRouteRecordsToMatchingOutputStreams() {
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

        var branches = streams.from(IncomingOrder.class)
                .branch(
                        order -> order.customer().startsWith("A"),
                        order -> true
                );
        branches.get(0).to("orders.a-customers");
        var topology = branches.get(1).to("orders.other-customers");

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var aCustomersTopic = driver.createOutputTopic("orders.a-customers", new StringDeserializer(),
                    new ByteArrayDeserializer());
            var otherCustomersTopic = driver.createOutputTopic("orders.other-customers", new StringDeserializer(),
                    new ByteArrayDeserializer());

            inputTopic.pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
            inputTopic.pipeInput("o-2", new IncomingOrder("o-2", "Bob"));
            inputTopic.pipeInput("o-3", new IncomingOrder("o-3", "Anna"));

            assertThat(aCustomersTopic.readValuesToList()).hasSize(2);
            assertThat(otherCustomersTopic.readValuesToList()).hasSize(1);
        }
    }

    @Test
    void merge_shouldCombineRecordsFromTwoBranchesIntoSingleOutput() {
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

        var branches = streams.from(IncomingOrder.class)
                .branch(
                        order -> order.customer().startsWith("A"),
                        order -> true
                );

        var topology = branches.get(0)
                .map(order -> new ProcessedOrder(order.orderId(), "A:" + order.customer()))
                .merge(branches.get(1).map(order -> new ProcessedOrder(order.orderId(), "B:" + order.customer())))
                .to(ProcessedOrder.class);

        try (var driver = new TopologyTestDriver(topology.nativeTopology(), streamsConfig())) {
            var inputTopic = driver.createInputTopic("orders.in", new StringSerializer(), fixture.serializer);
            var outputTopic = driver.createOutputTopic("orders.out", new StringDeserializer(), fixture.deserializer);

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
}



