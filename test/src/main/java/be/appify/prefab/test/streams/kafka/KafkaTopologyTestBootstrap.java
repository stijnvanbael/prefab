package be.appify.prefab.test.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaSchemaRegistrySupport;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.TopologyElement;
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
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
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
        KafkaSchemaRegistrySupport.enableMockSchemaRegistry(kafkaProperties);
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

    /**
     * Starts a test session for a topology made of stream definitions and/or global stores.
     *
     * <p>All elements must originate from the same {@link PrefabStreams} instance. A topology that consists of
     * global stores only is allowed.
     *
     * @param elements the stream definitions and global stores that make up the topology
     * @return the test session
     */
    public TopologyTestSession run(TopologyElement... elements) {
        var properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        return new TopologyTestSession(
                new TopologyTestDriver(sharedTopology(elements), properties),
                eventRegistry,
                serializer,
                deserializer,
                jsonMapper);
    }

    private static Topology sharedTopology(TopologyElement[] elements) {
        if (elements.length == 0) {
            throw new IllegalArgumentException("At least one stream definition or global store is required");
        }
        var first = elements[0].nativeTopology();
        var description = first.describe().toString();
        if (Arrays.stream(elements).anyMatch(element -> !element.nativeTopology().describe().toString().equals(description))) {
            throw new IllegalArgumentException(
                    "Topology elements must originate from the same PrefabStreams instance to run in one topology");
        }
        return first;
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
            return driver.createInputTopic(topic, new StringKeySerde<>(keyTypeOf(type)).serializer(), serializer.adapt());
        }

        public <K, V extends Keyed<K>> TestOutputTopic<K, V> output(Class<V> type) {
            var topic = eventRegistry().topicForType(type);
            return driver.createOutputTopic(topic, new StringKeySerde<>(keyTypeOf(type)).deserializer(), deserializer.adapt());
        }

        /**
         * Gives access to the content of a global store, e.g. to assert on what an updater has written.
         *
         * @param store the global store declared on the topology under test
         * @return a view on the store content
         */
        public <K, V> KeyValueStore<K, V> store(GlobalStore<K, V> store) {
            return driver.getKeyValueStore(store.name());
        }

        @Override
        public void close() {
            driver.close();
        }
    }
}
