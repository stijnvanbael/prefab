package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import be.appify.prefab.core.util.SerializationRegistry;
import java.util.Properties;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            var outputTopic = driver.createOutputTopic("orders.dead-letter", new StringDeserializer(), new ByteArrayDeserializer());

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
                .hasMessageContaining("No Kafka topic registered for type");
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
                .hasMessageContaining("Multiple Kafka topics registered for type");
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
        var typeResolver = new KafkaJsonTypeResolver();
        var conversionService = new DefaultConversionService();
        var kafkaProperties = new KafkaProperties();
        var serializer = new DynamicSerializer(kafkaProperties, conversionService, serializationRegistry);
        var deserializer = new DynamicDeserializer(kafkaProperties, conversionService, serializationRegistry, typeResolver);
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

    record AuditOrder(String orderId, String status) {
    }

    private record Fixture(
            SerializationRegistry serializationRegistry,
            KafkaJsonTypeResolver typeResolver,
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



