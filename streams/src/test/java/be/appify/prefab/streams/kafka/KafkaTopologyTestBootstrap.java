package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.StreamDefinition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Properties;
import java.util.UUID;

final class KafkaTopologyTestBootstrap {
    private final EventRegistry typeRegistry = new EventRegistry();
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;

    private KafkaTopologyTestBootstrap() {
        var conversionService = new DefaultConversionService();
        var kafkaProperties = new KafkaProperties();
        serializer = new DynamicSerializer(kafkaProperties, conversionService, typeRegistry);
        deserializer = new DynamicDeserializer(kafkaProperties, conversionService, typeRegistry);
    }

    static KafkaTopologyTestBootstrap bootstrap() {
        return new KafkaTopologyTestBootstrap();
    }

    void registerJson(String topic, Class<?> type) {
        typeRegistry.register(topic, type, Event.Serialization.JSON);
    }

    KafkaPrefabStreams streams(StreamsBuilder streamsBuilder) {
        return new KafkaPrefabStreams(streamsBuilder, new KafkaTopicResolver(typeRegistry), serializer, deserializer);
    }

    TopologyTestSession run(StreamDefinition streamDefinition) {
        var properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "prefab-streams-topology-test-" + UUID.randomUUID());
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        return new TopologyTestSession(new TopologyTestDriver(streamDefinition.nativeTopology(), properties),
                serializer,
                deserializer);
    }

    record TopologyTestSession(
            TopologyTestDriver driver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) implements AutoCloseable {

        TestInputTopic<String, Object> input(String topic) {
            return driver.createInputTopic(topic, new StringSerializer(), serializer);
        }

        TestOutputTopic<String, Object> output(String topic) {
            return driver.createOutputTopic(topic, new StringDeserializer(), deserializer);
        }

        TestOutputTopic<String, byte[]> rawOutput(String topic) {
            return driver.createOutputTopic(topic, new StringDeserializer(), new ByteArrayDeserializer());
        }

        @Override
        public void close() {
            driver.close();
        }
    }
}

