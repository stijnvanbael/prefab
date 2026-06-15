package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
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

final class KafkaTopologyTestBootstrap {
    private final EventRegistry eventRegistry = new EventRegistry();
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;

    private KafkaTopologyTestBootstrap() {
        var conversionService = new DefaultConversionService();
        var kafkaProperties = new KafkaProperties();
        serializer = new DynamicSerializer(kafkaProperties, conversionService, eventRegistry);
        deserializer = new DynamicDeserializer(kafkaProperties, conversionService, eventRegistry);
    }

    static KafkaTopologyTestBootstrap bootstrap() {
        return new KafkaTopologyTestBootstrap();
    }

    PrefabStreams streams() {
        return new AutoRegisterPrefabStreamsTestDecorator(new KafkaPrefabStreams(new StreamsBuilder(), new KafkaTopicResolver(eventRegistry), serializer, deserializer), eventRegistry);
    }

    TopologyTestSession run(StreamDefinition streamDefinition) {
        var properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "prefab-streams-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        return new TopologyTestSession(
                new TopologyTestDriver(streamDefinition.nativeTopology(), properties),
                eventRegistry,
                serializer,
                deserializer);
    }

    record TopologyTestSession(
            TopologyTestDriver driver,
            EventRegistry eventRegistry,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) implements AutoCloseable {

        <T> TestInputTopic<String, T> input(Class<T> type) {
            var topic = eventRegistry().topicForType(type);
            return driver.createInputTopic(topic, new StringSerializer(), (Serializer<T>) serializer);
        }

        <T> TestOutputTopic<String, T> output(Class<T> type) {
            var topic = eventRegistry().topicForType(type);
            return driver.createOutputTopic(topic, new StringDeserializer(), (Deserializer<T>) deserializer);
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

