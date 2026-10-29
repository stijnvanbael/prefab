package be.appify.prefab.streams.kafka.testing;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaSchemaRegistrySupport;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.kafka.KafkaPrefabStreams;
import be.appify.prefab.streams.kafka.KafkaStateStoreAccess;
import be.appify.prefab.streams.kafka.KafkaTopicResolver;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestHarness;
import java.util.Properties;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

/** Runs topologies in-process with Kafka Streams' {@link TopologyTestDriver}. */
public final class KafkaTopologyTestHarness implements TopologyTestHarness {
    private final EventRegistry eventRegistry = new EventRegistry();
    private final StreamsBuilder streamsBuilder = new StreamsBuilder();
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;
    private final AutoRegisterPrefabStreamsTestDecorator streams;
    private final String appId;

    /**
     * Constructs a new KafkaTopologyTestHarness.
     *
     * @param appId the application id, used to derive topic names
     */
    public KafkaTopologyTestHarness(String appId) {
        this.appId = appId;
        var conversionService = new DefaultConversionService();
        var kafkaProperties = new KafkaProperties();
        KafkaSchemaRegistrySupport.enableMockSchemaRegistry(kafkaProperties);
        serializer = new DynamicSerializer(kafkaProperties, conversionService, eventRegistry);
        deserializer = new DynamicDeserializer(kafkaProperties, conversionService, eventRegistry);
        streams = new AutoRegisterPrefabStreamsTestDecorator(
                new KafkaPrefabStreams(
                        streamsBuilder,
                        new KafkaTopicResolver(eventRegistry),
                        serializer,
                        deserializer,
                        JsonMapper.builder().findAndAddModules().build(),
                        conversionService,
                        kafkaProperties.buildProducerProperties()),
                eventRegistry,
                appId);
    }

    @Override
    public PrefabStreams streams() {
        return streams;
    }

    @Override
    public TopologySession start() {
        var properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        var topology = streamsBuilder.build();
        var driver = new TopologyTestDriver(topology, properties);
        streams.globalStores().stream()
                .filter(KafkaStateStoreAccess.class::isInstance)
                .map(KafkaStateStoreAccess.class::cast)
                .forEach(store -> store.attach(driver.getKeyValueStore(store.storeName())));
        return new KafkaTopologySession(topology, driver, eventRegistry, serializer, deserializer);
    }

    /** Registers {@link KafkaTopologyTestHarness} as the harness for Kafka Streams. */
    public static final class Factory implements TopologyTestHarness.Factory {
        @Override
        public TopologyTestHarness create(String appId) {
            return new KafkaTopologyTestHarness(appId);
        }
    }
}

