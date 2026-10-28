package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaSchemaRegistrySupport;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
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

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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

    public TopologyTestSession run(StreamDefinition... streamDefinitions) {
        return run(List.of(streamDefinitions), List.of());
    }

    /**
     * Starts a test session for streams that share one {@link PrefabStreams} instance.
     *
     * @param streamDefinitions the definitions to run; they must originate from the same {@link PrefabStreams}
     * @param globalStores global stores that must be part of the topology, so they can be inspected in the session
     * @return the test session
     */
    public TopologyTestSession run(
            Collection<StreamDefinition> streamDefinitions,
            Collection<GlobalStore<?, ?>> globalStores
    ) {
        var topology = sharedTopology(streamDefinitions);
        var declared = topology.describe().globalStores().stream()
                .flatMap(store -> store.processor().stores().stream())
                .collect(Collectors.toSet());
        globalStores.stream()
                .filter(store -> !declared.contains(store.name()))
                .findFirst()
                .ifPresent(store -> {
                    throw new IllegalArgumentException(
                            "Global store '%s' is not part of the topology; declare it on the PrefabStreams that created the stream definitions"
                                    .formatted(store.name()));
                });
        var properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        return new TopologyTestSession(
                new TopologyTestDriver(topology, properties),
                eventRegistry,
                serializer,
                deserializer,
                jsonMapper);
    }

    private static Topology sharedTopology(Collection<StreamDefinition> streamDefinitions) {
        if (streamDefinitions.isEmpty()) {
            throw new IllegalArgumentException("At least one stream definition is required");
        }
        var topologies = streamDefinitions.stream().map(StreamDefinition::nativeTopology).toList();
        var first = topologies.getFirst();
        var description = first.describe().toString();
        if (topologies.stream().anyMatch(topology -> !topology.describe().toString().equals(description))) {
            throw new IllegalArgumentException(
                    "Stream definitions must originate from the same PrefabStreams instance to run in one topology");
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
