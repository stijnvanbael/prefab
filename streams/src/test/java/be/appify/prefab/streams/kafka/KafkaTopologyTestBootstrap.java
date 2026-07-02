package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import java.util.Properties;

import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.keyTypeOf;

public final class KafkaTopologyTestBootstrap {
    private final EventRegistry eventRegistry = new EventRegistry();
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;
    private final JsonMapper jsonMapper;
    private final DefaultConversionService conversionService;
    private final KafkaProperties kafkaProperties;
    private final String appId;

    private KafkaTopologyTestBootstrap(String appId) {
        this.appId = appId;
        this.conversionService = new DefaultConversionService();
        this.kafkaProperties = new KafkaProperties();
        serializer = new DynamicSerializer(kafkaProperties, conversionService, eventRegistry);
        deserializer = new DynamicDeserializer(kafkaProperties, conversionService, eventRegistry);
        this.jsonMapper = JsonMapper.builder().findAndAddModules().build();
    }
    public static KafkaTopologyTestBootstrap bootstrap() {
        return bootstrap("test");
    }

    public static KafkaTopologyTestBootstrap bootstrap(String appId) {
        return new KafkaTopologyTestBootstrap(appId);
    }

    public PrefabStreams streams() {
        return new AutoRegisterPrefabStreamsTestDecorator(
                new KafkaPrefabStreams(
                        new StreamsBuilder(),
                        new KafkaTopicResolver(eventRegistry),
                        serializer,
                        deserializer,
                        jsonMapper,
                        conversionService,
                        kafkaProperties.buildProducerProperties()),
                eventRegistry,
                appId);
    }

    public TopologyTestSession run(StreamDefinition streamDefinition) {
        var properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        return new TopologyTestSession(
                new TopologyTestDriver(streamDefinition.nativeTopology(), properties),
                eventRegistry,
                serializer,
                deserializer,
                jsonMapper);
    }

    public record TopologyTestSession(
            TopologyTestDriver driver,
            EventRegistry eventRegistry,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper
    ) implements AutoCloseable {

        public <K, V extends Keyed<K>> TestInputTopic<K, V> input(Class<V> type) {
            var topic = eventRegistry().topicForType(type);
            var keyType = keyTypeOf(type);
            return driver.createInputTopic(topic, new StringKeySerde<>(keyType).serializer(), serializer.adapt());
        }

        <K, T> TestInputTopic<K, T> input(Class<T> type, Class<K> keyType) {
            var topic = eventRegistry().topicForType(type);
            return driver.createInputTopic(topic, new StringKeySerde<>(keyType).serializer(), (Serializer<T>) serializer);
        }

        public <K, V extends Keyed<K>> TestOutputTopic<K, V> output(Class<V> type) {
            var topic = eventRegistry().topicForType(type);
            var keyType = keyTypeOf(type);
            return driver.createOutputTopic(topic, new StringKeySerde<>(keyType).deserializer(), deserializer.adapt());
        }

        <K, T> TestOutputTopic<K, T> output(Class<T> type, Class<K> keyType) {
            var topic = eventRegistry().topicForType(type);
            return driver.createOutputTopic(topic, new StringKeySerde<>(keyType).deserializer(), (Deserializer<T>) deserializer);
        }

        TestOutputTopic<String, byte[]> rawOutput(String topic) {
            return driver.createOutputTopic(topic, new StringDeserializer(), new ByteArrayDeserializer());
        }

        TestOutputTopic<byte[], byte[]> rawOutputBytes(String topic) {
            return driver.createOutputTopic(topic, new ByteArrayDeserializer(), new ByteArrayDeserializer());
        }

        @Override
        public void close() {
            driver.close();
        }
    }
}
