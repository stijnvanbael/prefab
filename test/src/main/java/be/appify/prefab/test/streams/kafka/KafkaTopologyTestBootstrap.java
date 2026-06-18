package be.appify.prefab.test.streams.kafka;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.kafka.KafkaPrefabStreams;
import be.appify.prefab.streams.kafka.KafkaTopicResolver;
import be.appify.prefab.streams.kafka.StringKeySerde;
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

import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.keyTypeOf;

public final class KafkaTopologyTestBootstrap {
    private final EventRegistry eventRegistry = new EventRegistry();
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;
    private final String appId;

    private KafkaTopologyTestBootstrap(String appId) {
        this.appId = appId;
        var conversionService = new DefaultConversionService();
        var kafkaProperties = new KafkaProperties();
        serializer = new DynamicSerializer(kafkaProperties, conversionService, eventRegistry);
        deserializer = new DynamicDeserializer(kafkaProperties, conversionService, eventRegistry);
    }
    public static KafkaTopologyTestBootstrap bootstrap() {
        return bootstrap("test");
    }

    public static KafkaTopologyTestBootstrap bootstrap(String appId) {
        return new KafkaTopologyTestBootstrap(appId);
    }

    public PrefabStreams streams() {
        return new AutoRegisterPrefabStreamsTestDecorator(
                new KafkaPrefabStreams(new StreamsBuilder(), new KafkaTopicResolver(eventRegistry), serializer, deserializer),
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
                deserializer);
    }

    public record TopologyTestSession(
            TopologyTestDriver driver,
            EventRegistry eventRegistry,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) implements AutoCloseable {

        public <K extends Key<K>, V extends Keyed<K>> TestInputTopic<K, V> input(Class<V> type) {
            var topic = eventRegistry().topicForType(type);
            return driver.createInputTopic(topic, new StringKeySerde<>(keyTypeOf(type)).serializer(), serializer.adapt());
        }

        public <K extends Key<K>, V extends Keyed<K>> TestOutputTopic<K, V> output(Class<V> type) {
            var topic = eventRegistry().topicForType(type);
            return driver.createOutputTopic(topic, new StringKeySerde<>(keyTypeOf(type)).deserializer(), deserializer.adapt());
        }

        @Override
        public void close() {
            driver.close();
        }
    }
}

