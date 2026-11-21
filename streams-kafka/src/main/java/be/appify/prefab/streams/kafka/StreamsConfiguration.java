package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaConfiguration;
import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.tracing.TracingConfig;
import java.util.HashMap;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import tools.jackson.databind.json.JsonMapper;

/** Kafka-backed baseline streams DSL wiring. */
@AutoConfiguration(after = KafkaConfiguration.class)
@ConditionalOnClass(StreamsBuilder.class)
@ConditionalOnBean({PrefabCoreConfiguration.class, DynamicSerializer.class})
@EnableKafkaStreams
public class StreamsConfiguration {

    /** Constructs a new StreamsConfiguration. */
    public StreamsConfiguration() {
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    KafkaStreamsConfiguration defaultStreamsConfig(
            KafkaConnectionDetails connectionDetails,
            @Value("${spring.kafka.streams.application-id:${spring.application.name}}") String applicationId,
            @Value("${prefab.streams.processing-guarantee:at_least_once}") String processingGuarantee
    ) {
        var properties = new HashMap<String, Object>();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducer().getBootstrapServers());
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        properties.put(StreamsConfig.STATE_DIR_CONFIG, "target/kafka-streams-state");
        properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, resolveProcessingGuarantee(processingGuarantee));
        return new KafkaStreamsConfiguration(properties);
    }

    /**
     * Resolves the Kafka Streams processing guarantee from the {@code prefab.streams.processing-guarantee} property.
     * Accepted values are {@code at_least_once} (default) and {@code exactly_once_v2}; hyphens are accepted too.
     */
    static String resolveProcessingGuarantee(String configured) {
        var normalized = configured == null ? "" : configured.trim().toLowerCase().replace('-', '_');
        return switch (normalized) {
            case "", StreamsConfig.AT_LEAST_ONCE -> StreamsConfig.AT_LEAST_ONCE;
            case StreamsConfig.EXACTLY_ONCE_V2 -> StreamsConfig.EXACTLY_ONCE_V2;
            default -> throw new IllegalArgumentException(
                    "Unsupported prefab.streams.processing-guarantee '%s'; use '%s' or '%s'"
                            .formatted(configured, StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2));
        };
    }
    @Bean
    KafkaTopicResolver kafkaTopicResolver(EventRegistry eventRegistry) {
        return new KafkaTopicResolver(eventRegistry);
    }

    @Bean
    TracingConfig streamsTracingConfig(
            @Value("${prefab.streams.tracing.enabled:false}") boolean enabled,
            @Value("${prefab.streams.tracing.max-correlation-ids:100}") int maxCorrelationIds
    ) {
        return new TracingConfig(enabled, maxCorrelationIds);
    }

    @Bean
    PrefabStreams prefabStreams(
            StreamsBuilder streamsBuilder,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper,
            ConversionService conversionService,
            KafkaProperties kafkaProperties,
            TracingConfig tracingConfig
    ) {
        return new KafkaPrefabStreams(
                streamsBuilder,
                topicResolver,
                serializer,
                deserializer,
                jsonMapper,
                conversionService,
                kafkaProperties.buildProducerProperties(),
                tracingConfig);
    }

    @Bean
    SmartInitializingSingleton streamTopologyBootstrap(ObjectProvider<StreamDefinition> streamDefinitions) {
        return () -> streamDefinitions.orderedStream().forEach(this::buildTopology);
    }

    private void buildTopology(StreamDefinition definition) {
        if (definition instanceof KafkaStreamDefinition kafkaDefinition) {
            kafkaDefinition.buildTopology();
        }
    }
}



